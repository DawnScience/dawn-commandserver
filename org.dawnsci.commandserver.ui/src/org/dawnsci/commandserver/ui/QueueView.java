package org.dawnsci.commandserver.ui;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.util.io.PropUtils;
import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.StatusBean;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A view for which the secondary id MUST be set and provides the queueName
 * and optionally the queue view name if a custom one is required. Syntax of
 * these parameters in the secondary id are key1=value1;key2=value2...
 * 
 * The essential keys are: uri, queueName, beanBundleName, beanClassName
 * The optional keys are: partName
 * 
 * Example id for this view would be:
 * org.dawnsci.commandserver.ui.queueView:uri=tcp%3A//ws097.diamond.ac.uk%3A61616;queueName=scisoft.xia2.STATUS_QUEUE;partName=XIA2 Reprocessing;beanClassName=org.dawnsci.commandserver.mx.beans.DataCollectionsBean
 * 
 * You can optionally extend this class to provide a table which is displayed for your
 * queue of custom objects. For instance for a queue showing xia2 reruns, the 
 * extra columns for this could be defined. However by default the 
 * 
 * @author fcp94556
 *
 */
public class QueueView extends ViewPart {
	
	private TableViewer viewer;
	private Properties idProperties;

	@Override
	public void createPartControl(Composite content) {
		
		content.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(content);

		this.viewer   = new TableViewer(content, SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.setUseHashlookup(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		createColumns();
		viewer.setContentProvider(createContentProvider());
		
		viewer.setInput(getUri());
		
		String name = getSecondaryIdAttribute("partName");
        if (name!=null) setPartName(name);
		
        createActions();
	}
	
	private void createActions() {
		final IContributionManager man = getViewSite().getActionBars().getToolBarManager();
	
		final Action refresh = new Action("Refresh") {
			public void run() {
				refresh();
			}
		};
		
		man.add(refresh);

		final Action configure = new Action("Configure...") {
			public void run() {
				PropertiesDialog dialog = new PropertiesDialog(getSite().getShell(), idProperties);
				
				int ok = dialog.open();
				if (ok == PropertiesDialog.OK) {
					idProperties.clear();
					idProperties.putAll(dialog.getProps());
				}
				refresh();
			}
		};
		
		man.add(configure);
	}

	protected void refresh() {
		viewer.setInput(getUri());
		viewer.refresh();
	}
	
	private IContentProvider createContentProvider() {
		return new IStructuredContentProvider() {
			
			private List<? extends StatusBean> queue;
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				final String uri   = (String)newInput;
				try {
					queue = readQueue(uri);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void dispose() {
				if (queue!=null) queue.clear();
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return queue.toArray(new StatusBean[queue.size()]);
			}
		};
	}

	protected List<StatusBean> readQueue(final String uri) throws Exception {
		
		QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
		QueueConnection qCon  = connectionFactory.createQueueConnection(); 
		QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue   = qSes.createQueue(getQueueName());
		qCon.start();
		
		final List<StatusBean> ret = new ArrayList<StatusBean>(7);
	    QueueBrowser qb = qSes.createBrowser(queue);
	    
	    @SuppressWarnings("rawtypes")
		Enumeration  e  = qb.getEnumeration();
	    
	    String beanBundleName = getSecondaryIdAttribute("beanBundleName");
	    String beanClassName  = getSecondaryIdAttribute("beanClassName");
	    
	    @SuppressWarnings("rawtypes")
	    Bundle bundle = Platform.getBundle(beanBundleName);
		Class clazz = bundle.loadClass(beanClassName);

		ObjectMapper mapper = new ObjectMapper();
        while(e.hasMoreElements()) {
	    	Message m = (Message)e.nextElement();
	    	if (m==null) continue;
        	if (m instanceof TextMessage) {
            	TextMessage t = (TextMessage)m;
              	@SuppressWarnings("unchecked")
				final StatusBean bean = mapper.readValue(t.getText(), clazz);
            	ret.add(bean);
        	}
	    }
        return ret;
	}

	protected void createColumns() {
		
		final TableViewerColumn name = new TableViewerColumn(viewer, SWT.LEFT);
		name.getColumn().setText("Name");
		name.getColumn().setWidth(300);
		name.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((StatusBean)element).getName();
			}
		});
		
		final TableViewerColumn status = new TableViewerColumn(viewer, SWT.CENTER);
		status.getColumn().setText("Status");
		status.getColumn().setWidth(100);
		status.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((StatusBean)element).getStatus().toString();
			}
		});

		final TableViewerColumn pc = new TableViewerColumn(viewer, SWT.CENTER);
		pc.getColumn().setText("Complete (%)");
		pc.getColumn().setWidth(120);
		pc.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return String.valueOf(((StatusBean)element).getPercentComplete());
			}
		});

	}

	@Override
	public void setFocus() {
		if (!viewer.getTable().isDisposed()) {
			viewer.getTable().setFocus();
		}
	}

	protected String getUri() {
		final String uri = getSecondaryIdAttribute("uri");
		if (uri == null) return null;
		return uri.replace("%3A", ":");
	}

	protected String getQueueName() {
		return getSecondaryIdAttribute("queueName");
	}
	
	private String getSecondaryIdAttribute(String key) {
		if (idProperties!=null) return idProperties.getProperty(key);
		if (getViewSite()==null) return null;
		final String secondId = getViewSite().getSecondaryId();
		if (secondId == null) return null;
		idProperties = PropUtils.parseString(secondId);
		return idProperties.getProperty(key);
	}
}
