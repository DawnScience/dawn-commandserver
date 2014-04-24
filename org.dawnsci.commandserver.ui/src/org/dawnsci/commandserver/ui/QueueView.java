package org.dawnsci.commandserver.ui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.dawb.common.ui.util.GridUtils;
import org.dawb.common.util.io.PropUtils;
import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A view for which the secondary id MUST be set and provides the queueName
 * and optionally the queue view name if a custom one is required. Syntax of
 * these parameters in the secondary id are key1=value1;key2=value2...
 * 
 * The essential keys are: uri, queueName, beanBundleName, beanClassName, topicName
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
	
	private static final Logger logger = LoggerFactory.getLogger(QueueView.class);
	
	// UI
	private TableViewer                       viewer;
	
	// Data
	private Properties                        idProperties;
	private Map<String, StatusBean>           queue;

	private Connection topicConnection;

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
        try {
			createTopicListener(getUri());
		} catch (Exception e) {
			logger.error("Cannot listen to topic of command server!", e);
		}
	}
	
	/**
	 * Listens to a topic
	 */
	private void createTopicListener(final String uri) throws Exception {
		
		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
        topicConnection = connectionFactory.createConnection();
        topicConnection.start();

        Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        final Topic           topic    = session.createTopic(getTopicName());
        final MessageConsumer consumer = session.createConsumer(topic);

        final Class        clazz  = getBeanClass();
        final ObjectMapper mapper = new ObjectMapper();
        
        MessageListener listener = new MessageListener() {
            public void onMessage(Message message) {
            	
            	if (viewer.getTable().isDisposed()) {
            		try {
						consumer.setMessageListener(null);
					} catch (JMSException e) {
						logger.warn("Cannot reset message listener (not a fatal message).", e);
					}
            		return;
            	}
                try {
                    if (message instanceof TextMessage) {
                        TextMessage t = (TextMessage) message;
        				final StatusBean bean = mapper.readValue(t.getText(), clazz);
                        mergeBean(bean);
                    }
                } catch (Exception e) {
                    logger.error("Updating changed bean from topic", e);
                }
            }
        };
        consumer.setMessageListener(listener);
	}
	
	public void dispose() {
		super.dispose();
		try {
			if (topicConnection!=null) topicConnection.close();
		} catch (Exception ne) {
			logger.warn("Problem stopping topic listening for "+getTopicName(), ne);
		}
	}

	/**
	 * Updates the bean if it is found in the list, otherwise
	 * refreshes the whole list because a bean we are not reporting
	 * has been(bean?) encountered.
	 * 
	 * @param bean
	 */
	protected void mergeBean(final StatusBean bean) throws Exception {
		
		getSite().getShell().getDisplay().asyncExec(new Runnable() {
			public void run(){
				if (queue.containsKey(bean.getUniqueId())) {
					queue.get(bean.getUniqueId()).merge(bean);
					viewer.refresh();
				} else {
					reconnect();
				}
			}
		});
	}

	private void createActions() {
		final IContributionManager man = getViewSite().getActionBars().getToolBarManager();
	
		final Action refresh = new Action("Refresh", Activator.getDefault().getImageDescriptor("icons/arrow-circle-double-135.png")) {
			public void run() {
				reconnect();
			}
		};
		
		man.add(refresh);

		final Action configure = new Action("Configure...", Activator.getDefault().getImageDescriptor("icons/document--pencil.png")) {
			public void run() {
				PropertiesDialog dialog = new PropertiesDialog(getSite().getShell(), idProperties);
				
				int ok = dialog.open();
				if (ok == PropertiesDialog.OK) {
					idProperties.clear();
					idProperties.putAll(dialog.getProps());
				}
				reconnect();
			}
		};
		
		man.add(configure);
	}

	protected void reconnect() {
		viewer.setInput(getUri());
		viewer.refresh();
	}
	
	private IContentProvider createContentProvider() {
		return new IStructuredContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				final String uri   = (String)newInput;
				try {
					queue = readQueue(uri);
				} catch (Exception e) {
                    logger.error("Updating changed bean from topic", e);
				}
			}
			
			@Override
			public void dispose() {
				if (queue!=null) queue.clear();
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return queue.values().toArray(new StatusBean[queue.size()]);
			}
		};
	}

	protected Map<String, StatusBean> readQueue(final String uri) throws Exception {
		
		QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
		QueueConnection qCon  = connectionFactory.createQueueConnection(); 
		QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue   = qSes.createQueue(getQueueName());
		qCon.start();
		
		final Map<String,StatusBean> ascending = new LinkedHashMap<String,StatusBean>();
	    QueueBrowser qb = qSes.createBrowser(queue);
	    
	    @SuppressWarnings("rawtypes")
		Enumeration  e  = qb.getEnumeration();
	    
        Class        clazz  = getBeanClass();
		ObjectMapper mapper = new ObjectMapper();
        while(e.hasMoreElements()) {
	    	Message m = (Message)e.nextElement();
	    	if (m==null) continue;
        	if (m instanceof TextMessage) {
            	TextMessage t = (TextMessage)m;
              	@SuppressWarnings("unchecked")
				final StatusBean bean = mapper.readValue(t.getText(), clazz);
            	ascending.put(bean.getUniqueId(), bean);
        	}
	    }
        
        // We reverse the queue because it comes out date ascending and we
        // want newest submissions first.
		final Map<String,StatusBean> decending = new LinkedHashMap<String,StatusBean>();
        final List<String> keys = new ArrayList<String>(ascending.keySet());
        for (int i = keys.size()-1; i > -1; i--) {
        	String key = keys.get(i);
        	decending.put(key, ascending.get(key));
		}
        return decending;
	}

	private Class getBeanClass() throws ClassNotFoundException {
	    String beanBundleName = getSecondaryIdAttribute("beanBundleName");
	    String beanClassName  = getSecondaryIdAttribute("beanClassName");
	    
	    @SuppressWarnings("rawtypes")
	    Bundle bundle = Platform.getBundle(beanBundleName);
		return bundle.loadClass(beanClassName);
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

		final TableViewerColumn submittedDate = new TableViewerColumn(viewer, SWT.CENTER);
		submittedDate.getColumn().setText("Date Submitted");
		submittedDate.getColumn().setWidth(200);
		submittedDate.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return DateFormat.getDateTimeInstance().format(new Date(((StatusBean)element).getSubmissionTime()));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

	}

	@Override
	public void setFocus() {
		if (!viewer.getTable().isDisposed()) {
			viewer.getTable().setFocus();
		}
	}


	private String getTopicName() {
		return getSecondaryIdAttribute("topicName");
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
