package org.dawnsci.commandserver.ui.view;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
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
import org.dawnsci.commandserver.core.util.JSONUtils;
import org.dawnsci.commandserver.ui.Activator;
import org.dawnsci.commandserver.ui.dialog.PropertiesDialog;
import org.dawnsci.commandserver.ui.preference.CommandConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
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
 * The essential keys are: beanBundleName, beanClassName, queueName, topicName
 * You can use createId(...) to generate a legal id from them.
 * 
 * The optional keys are: partName, 
 *                        uri (default CommandConstants.JMS_URI)
 * 
 * Example id for this view would be:
 * org.dawnsci.commandserver.ui.queueView:beanClassName=org.dawnsci.commandserver.mx.beans.ProjectBean;beanBundleName=org.dawnsci.commandserver.mx
 * 
 * You can optionally extend this class to provide a table which is displayed for your
 * queue of custom objects. For instance for a queue showing xia2 reruns, the 
 * extra columns for this could be defined. However by default the 
 * 
 * @author fcp94556
 *
 */
public class StatusQueueView extends ViewPart {
	
	public static final String ID = "org.dawnsci.commandserver.ui.queueView";
	
	private static final Logger logger = LoggerFactory.getLogger(StatusQueueView.class);
	
	// UI
	private TableViewer                       viewer;
	
	// Data
	private Properties                        idProperties;
	private Map<String, StatusBean>           queue;

	private Connection topicConnection;

	private Action kill;

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
		
		updateQueue(getUri());
		
		String name = getSecondaryIdAttribute("partName");
        if (name!=null) setPartName(name);
		
        createActions();
        try {
			createTopicListener(getUri());
		} catch (Exception e) {
			logger.error("Cannot listen to topic of command server!", e);
		}
        
		getViewSite().setSelectionProvider(viewer);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {	
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final StatusBean bean = getSelection();
				boolean enabled = true;
				if (bean==null) enabled = false;
				if (bean!=null) enabled = !bean.getStatus().isFinal();
				kill.setEnabled(enabled);
			}
		});

	}
	
	/**
	 * Listens to a topic
	 */
	private void createTopicListener(final String uri) throws Exception {
		
		// Use job because connection might timeout.
		final Job topicJob = new Job("Create topic listener") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			        StatusQueueView.this.topicConnection = connectionFactory.createConnection();
			        topicConnection.start();
	
			        Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	
			        final Topic           topic    = session.createTopic(getTopicName());
			        final MessageConsumer consumer = session.createConsumer(topic);
	
			        final Class        clazz  = getBeanClass();
			        final ObjectMapper mapper = new ObjectMapper();
			        
			        MessageListener listener = new MessageListener() {
			            public void onMessage(Message message) {		            	
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
			        return Status.OK_STATUS;
			        
				} catch (Exception ne) {
					logger.error("Cannot listen to topic changes because command server is not there", ne);
			        return Status.CANCEL_STATUS;
				}
			}
			
			
		};
		
		topicJob.setPriority(Job.INTERACTIVE);
		topicJob.setSystem(true);
		topicJob.setUser(false);
		topicJob.schedule();
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
		
		final IContributionManager toolMan = getViewSite().getActionBars().getToolBarManager();
		final MenuManager          menuMan = new MenuManager();
	
		this.kill = new Action("Terminate job", Activator.getDefault().getImageDescriptor("icons/terminate.png")) {
			public void run() {
				
				final StatusBean bean = getSelection();
				if (bean==null) return;
				
				if (bean.getStatus().isFinal()) {
					MessageDialog.openInformation(getViewSite().getShell(), "Run '"+bean.getName()+"' inactive", "Run '"+bean.getName()+"' is inactive and cannot be terminated.");
					return;
				}
				try {
					
					final DateFormat format = DateFormat.getDateTimeInstance();
					boolean ok = MessageDialog.openConfirm(getViewSite().getShell(), "Confirm terminate "+bean.getName(), 
							  "Are you sure you want to terminate "+bean.getName()+" submitted on "+format.format(new Date(bean.getSubmissionTime()))+"?");
					
					if (!ok) return;
					
					bean.setStatus(org.dawnsci.commandserver.core.beans.Status.REQUEST_TERMINATE);
					bean.setMessage("Requesting a termination of "+bean.getName());
					JSONUtils.sendTopic(bean, getTopicName(), getUri());
					
				} catch (Exception e) {
					ErrorDialog.openError(getViewSite().getShell(), "Cannot terminate "+bean.getName(), "Cannot terminate "+bean.getName()+"\n\nPlease contact your support representative.",
							new Status(IStatus.ERROR, "org.dawnsci.commandserver.ui", e.getMessage()));
				}
			}
		};
		toolMan.add(kill);
		menuMan.add(kill);
		
		final Action refresh = new Action("Refresh", Activator.getDefault().getImageDescriptor("icons/arrow-circle-double-135.png")) {
			public void run() {
				reconnect();
			}
		};
		
		toolMan.add(refresh);
		menuMan.add(refresh);

		final Action configure = new Action("Configure...", Activator.getDefault().getImageDescriptor("icons/document--pencil.png")) {
			public void run() {
				PropertiesDialog dialog = new PropertiesDialog(getSite().getShell(), idProperties);
				
				int ok = dialog.open();
				if (ok == PropertiesDialog.OK) {
					idProperties.clear();
					idProperties.putAll(dialog.getProps());
					reconnect();
				}
			}
		};
		
		toolMan.add(configure);
		menuMan.add(configure);
		
		viewer.getControl().setMenu(menuMan.createContextMenu(viewer.getControl()));
	}

	protected void reconnect() {
		updateQueue(getUri());
	}
	
	private IContentProvider createContentProvider() {
		return new IStructuredContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				queue = (Map<String, StatusBean>)newInput;
			}
			
			@Override
			public void dispose() {
				if (queue!=null) queue.clear();
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (queue==null) return new StatusBean[]{StatusBean.EMPTY};
				return queue.values().toArray(new StatusBean[queue.size()]);
			}
		};
	}
	
	protected StatusBean getSelection() {
		final ISelection sel = viewer.getSelection();
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection)sel;
			if (ss.size()>0) return (StatusBean)ss.getFirstElement();
		}
		return null;
	}

	/**
	 * Read Queue and return in submission order.
	 * @param uri
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected synchronized void updateQueue(final String uri) {
		

		final Job queueJob = new Job("Connect and read queue") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask("Connect to command server", 10);
					monitor.worked(1);
					QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
					monitor.worked(1);
					QueueConnection qCon  = connectionFactory.createQueueConnection(); // This times out when the server is not there.
					QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
					Queue queue   = qSes.createQueue(getQueueName());
					qCon.start();
					
				    QueueBrowser qb = qSes.createBrowser(queue);
					monitor.worked(1);
			    
				    @SuppressWarnings("rawtypes")
					Enumeration  e  = qb.getEnumeration();
				    
			        Class        clazz  = getBeanClass();
					ObjectMapper mapper = new ObjectMapper();
					
					final Collection<StatusBean> list = new TreeSet<StatusBean>(new Comparator<StatusBean>() {
						@Override
						public int compare(StatusBean o1, StatusBean o2) {
							// Newest first!
					        long t1 = o2.getSubmissionTime();
					        long t2 = o1.getSubmissionTime();
					        return (t1<t2 ? -1 : (t1==t2 ? 0 : 1));
						}
					});

			        while(e.hasMoreElements()) {
				    	Message m = (Message)e.nextElement();
				    	if (m==null) continue;
			        	if (m instanceof TextMessage) {
			            	TextMessage t = (TextMessage)m;
							final StatusBean bean = mapper.readValue(t.getText(), clazz);
			              	list.add(bean);
			        	}
				    }
					monitor.worked(1);
			        
			        // We reverse the queue because it comes out date ascending and we
			        // want newest submissions first.
					final Map<String,StatusBean> ret = new LinkedHashMap<String,StatusBean>();
			        for (StatusBean bean : list) {
			        	ret.put(bean.getUniqueId(), bean);
					}
					monitor.worked(1);
			        
			        getSite().getShell().getDisplay().syncExec(new Runnable() {
			        	public void run() {
			        		viewer.setInput(ret);
			        		viewer.refresh();
			        	}
			        });
			        monitor.done();
			        
			        return Status.OK_STATUS;
			        
				} catch (final Exception e) {
					
			        monitor.done();
			        logger.error("Updating changed bean from topic", e);
			        getSite().getShell().getDisplay().syncExec(new Runnable() {
			        	public void run() {
							ErrorDialog.openError(getViewSite().getShell(), "Cannot connect to queue", "The command server is unavailable.\n\nPlease contact your support representative.", 
						              new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage()));
			        	}
			        });
			        return Status.CANCEL_STATUS;

				}			
			}
			
		};
		queueJob.setPriority(Job.INTERACTIVE);
		queueJob.setUser(true);
		queueJob.schedule();


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
		name.getColumn().setWidth(260);
		name.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((StatusBean)element).getName();
			}
		});
		
		final TableViewerColumn status = new TableViewerColumn(viewer, SWT.LEFT);
		status.getColumn().setText("Status");
		status.getColumn().setWidth(140);
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
		submittedDate.getColumn().setWidth(150);
		submittedDate.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return DateFormat.getDateTimeInstance().format(new Date(((StatusBean)element).getSubmissionTime()));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});
		
		final TableViewerColumn message = new TableViewerColumn(viewer, SWT.LEFT);
		message.getColumn().setText("Message");
		message.getColumn().setWidth(150);
		message.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return ((StatusBean)element).getMessage();
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});
		
		final TableViewerColumn location = new TableViewerColumn(viewer, SWT.LEFT);
		location.getColumn().setText("Location");
		location.getColumn().setWidth(300);
		location.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					final StatusBean bean = (StatusBean)element;
		            return bean.getRunDirectory();
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
		final String topicName = getSecondaryIdAttribute("topicName");
		if (topicName != null) return topicName;
		return "scisoft.default.STATUS_TOPIC";
	}

    protected String getUri() {
		final String uri = getSecondaryIdAttribute("uri");
		if (uri != null) return uri.replace("%3A", ":");
		return getCommandPreference(CommandConstants.JMS_URI);
	}
    
    protected String getCommandPreference(String key) {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    	return store.getString(key);
    }

	protected String getQueueName() {
		final String qName =  getSecondaryIdAttribute("queueName");
		if (qName != null) return qName;
		return "scisoft.default.STATUS_QUEUE";
	}
	
	private String getSecondaryIdAttribute(String key) {
		if (idProperties!=null) return idProperties.getProperty(key);
		if (getViewSite()==null) return null;
		final String secondId = getViewSite().getSecondaryId();
		if (secondId == null) return null;
		idProperties = PropUtils.parseString(secondId);
		return idProperties.getProperty(key);
	}

	public static String createId(final String beanBundleName, final String beanClassName, final String queueName, final String topicName) {
		
		final StringBuilder buf = new StringBuilder();
		buf.append(ID);
		buf.append(":");
		buf.append(createSecondaryId(beanBundleName, beanClassName, queueName, topicName));
		return buf.toString();
	}
	
	public static String createSecondaryId(final String beanBundleName, final String beanClassName, final String queueName, final String topicName) {
		
		final StringBuilder buf = new StringBuilder();
		append(buf, "beanBundleName", beanBundleName);
		append(buf, "beanClassName",  beanClassName);
		append(buf, "queueName",      queueName);
		append(buf, "topicName",      topicName);
		return buf.toString();
	}

	private static void append(StringBuilder buf, String name, String value) {
		buf.append(name);
		buf.append("=");
		buf.append(value);
		buf.append(";");
	}
}
