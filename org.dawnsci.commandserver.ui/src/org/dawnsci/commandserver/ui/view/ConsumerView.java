package org.dawnsci.commandserver.ui.view;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.dawb.common.ui.util.GridUtils;
import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.beans.AdministratorMessage;
import org.dawnsci.commandserver.core.consumer.Constants;
import org.dawnsci.commandserver.core.consumer.ConsumerBean;
import org.dawnsci.commandserver.core.consumer.ConsumerStatus;
import org.dawnsci.commandserver.core.util.JSONUtils;
import org.dawnsci.commandserver.ui.Activator;
import org.dawnsci.commandserver.ui.preference.CommandConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A view which shows the active consumers available to process commands.
 * 
 * @author fcp94556
 *
 */
public class ConsumerView extends ViewPart {
	
	public static final String ID = "org.dawnsci.commandserver.ui.consumerView";
	
	private static final Logger logger = LoggerFactory.getLogger(ConsumerView.class);
	
	// UI
	private TableViewer                       viewer;
	
	// Data
	private Map<String, ConsumerBean>         consumers;

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
		
		consumers = new TreeMap<String, ConsumerBean>(Collections.reverseOrder());
		viewer.setInput(consumers);	
		
        createActions();
        try {
			createTopicListener(getUri());
		} catch (Exception e) {
			logger.error("Cannot listen to topic of command server!", e);
		}
        
        final Thread job = new Thread(new Runnable() {
			@Override
			public void run() {
				
                while(!viewer.getTable().isDisposed()) {
                	try {
						Thread.sleep(Constants.NOTIFICATION_FREQUENCY);
						if (viewer.getControl().isDisposed()) return;
						
						viewer.getControl().getDisplay().syncExec(new Runnable() {
							public void run () {
								viewer.refresh();
							}
						});
					} catch (InterruptedException e) {
						return;
					}
                }
 			}
        });
        
        job.setPriority(Thread.MIN_PRIORITY);
        job.setDaemon(true);
        job.setName("Refresh consumer table");
        job.start();
	}
	
	/**
	 * Listens to a topic
	 */
	private void createTopicListener(final URI uri) throws Exception {
		
		// Use job because connection might timeout.
		final Job topicJob = new Job("Create topic listener") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			        topicConnection = connectionFactory.createConnection();
			        topicConnection.start();
	
			        Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	
			        final Topic           topic    = session.createTopic(Constants.ALIVE_TOPIC);
			        final MessageConsumer consumer = session.createConsumer(topic);
	
			        final ObjectMapper mapper = new ObjectMapper();
			        
			        MessageListener listener = new MessageListener() {
			            public void onMessage(Message message) {		            	
			                try {
			                    if (message instanceof TextMessage) {
			                        TextMessage t = (TextMessage) message;
			        				final ConsumerBean bean = mapper.readValue(t.getText(), ConsumerBean.class);
			        				bean.setLastAlive(System.currentTimeMillis());
			                        consumers.put(bean.getConsumerId(), bean);
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
			logger.warn("Problem stopping topic listening for "+Constants.ALIVE_TOPIC, ne);
		}
	}

	private void createActions() {
		final IContributionManager man = getViewSite().getActionBars().getToolBarManager();
	
		final Action refresh = new Action("Refresh", Activator.getDefault().getImageDescriptor("icons/arrow-circle-double-135.png")) {
			public void run() {
				viewer.refresh();
			}
		};
		
		man.add(refresh);

		final Action stop = new Action("Stop consumer", Activator.getDefault().getImageDescriptor("icons/terminate.png")) {
			public void run() {
				
				if (  viewer.getSelection() == null || viewer.getSelection().isEmpty()) return;
				
			    ConsumerBean bean = (ConsumerBean)((IStructuredSelection)viewer.getSelection()).getFirstElement();

			    boolean ok = MessageDialog.openConfirm(getSite().getShell(), "Confirm Stop", "If you stop this consumer it will have to be restarted by an administrator.\n\n"
						                                                                      + "Are you sure that you want to do this?\n\n"
						                                                                      + "(NOTE: Long running jobs can be terminated without stopping the consumer!)");
			    if (!ok) return;
			    			    
			    boolean notify = MessageDialog.openQuestion(getSite().getShell(), "Warn Users", "Would you like to warn users before stopping the consumer?\n\n"
								                        + "If you say yes, a popup will open on users clients to warn about the imminent stop.");
                if (notify) {
                	
                	final AdministratorMessage msg = new AdministratorMessage();
                	msg.setTitle("'"+bean.getName()+"' will shutdown.");
                	msg.setMessage("'"+bean.getName()+"' is about to shutdown.\n\n"+
                	               "Any runs corrently running may loose progress notification,\n"+
                			       "however they should complete.\n\n"+
                	               "Runs yet to be started will be picked up when\n"+
                	               "'"+bean.getName()+"' restarts.");
                	try {
						JSONUtils.sendTopic(msg, Constants.ADMIN_MESSAGE_TOPIC, getUri());
					} catch (Exception e) {
						logger.error("Cannot notify of shutdown!", e);
					}
                }

				bean.setStatus(ConsumerStatus.REQUEST_TERMINATE);
				bean.setMessage("Requesting a termination of "+bean.getName());
				try {
					JSONUtils.sendTopic(bean, Constants.TERMINATE_CONSUMER_TOPIC, getUri());
				} catch (Exception e) {
					logger.error("Cannot terminate consumer "+bean.getName(), e);
				}

			}
		};
		
		man.add(stop);

		final MenuManager menuMan = new MenuManager();
		menuMan.add(refresh);
		menuMan.add(stop);
		
		viewer.getControl().setMenu(menuMan.createContextMenu(viewer.getControl()));
		
	}
	
	private IContentProvider createContentProvider() {
		return new IStructuredContentProvider() {
			
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {
				if (consumers!=null) consumers.clear();
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				if (consumers==null) return new ConsumerBean[]{ConsumerBean.EMPTY};
				return consumers.values().toArray(new ConsumerBean[consumers.size()]);
			}
		};
	}

	protected void createColumns() {
		
		final TableViewerColumn name = new TableViewerColumn(viewer, SWT.LEFT);
		name.getColumn().setText("Name");
		name.getColumn().setWidth(300);
		name.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				return ((ConsumerBean)element).getName();
			}
		});
		
		final TableViewerColumn status = new TableViewerColumn(viewer, SWT.CENTER);
		status.getColumn().setText("Status");
		status.getColumn().setWidth(100);
		status.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				final ConsumerBean cbean = (ConsumerBean)element;
				ConsumerStatus status = cbean.getStatus();
				if (cbean.getLastAlive()>(System.currentTimeMillis()-Constants.NOTIFICATION_FREQUENCY*10) && 
					cbean.getLastAlive()<(System.currentTimeMillis()-Constants.NOTIFICATION_FREQUENCY*2)) {
					status = ConsumerStatus.STOPPING;
					
				} else if (cbean.getLastAlive()<(System.currentTimeMillis()-Constants.NOTIFICATION_FREQUENCY*10)) {
					status = ConsumerStatus.STOPPED;
				}
				return status.toString();
			}
		});

		final TableViewerColumn startDate = new TableViewerColumn(viewer, SWT.CENTER);
		startDate.getColumn().setText("Date Started");
		startDate.getColumn().setWidth(150);
		startDate.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return DateFormat.getDateTimeInstance().format(new Date(((ConsumerBean)element).getStartTime()));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});
		
		final TableViewerColumn host = new TableViewerColumn(viewer, SWT.CENTER);
		host.getColumn().setText("Host");
		host.getColumn().setWidth(150);
		host.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return ((ConsumerBean)element).getHostName();
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		
		final TableViewerColumn lastAlive = new TableViewerColumn(viewer, SWT.CENTER);
		lastAlive.getColumn().setText("Last Alive");
		lastAlive.getColumn().setWidth(150);
		lastAlive.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					return DateFormat.getDateTimeInstance().format(new Date(((ConsumerBean)element).getLastAlive()));
				} catch (Exception e) {
					return e.getMessage();
				}
			}
		});

		final TableViewerColumn age = new TableViewerColumn(viewer, SWT.CENTER);
		age.getColumn().setText("Age");
		age.getColumn().setWidth(150);
		age.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				try {
					final ConsumerBean cbean = (ConsumerBean)element;
					return (new SimpleDateFormat("dd'd' mm'm' ss's'")).format(new Date(cbean.getLastAlive()-cbean.getStartTime()));
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


    protected URI getUri() throws Exception {
		return new URI(getCommandPreference(CommandConstants.JMS_URI));
	}
    
    protected String getCommandPreference(String key) {
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    	return store.getString(key);
    }
}
