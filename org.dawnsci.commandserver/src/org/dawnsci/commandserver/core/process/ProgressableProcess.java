package org.dawnsci.commandserver.core.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.util.JSONUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Platform;


/**
 * Extend to provide a connection between a running process 
 * and its 
 * 
 * @author fcp94556
 *
 */
public abstract class ProgressableProcess implements Runnable {

	private boolean            isCancelled = false;
	protected final StatusBean bean;
	protected final String     uri;
	protected final String     statusTName;
	protected final String     statusQName;

	public ProgressableProcess(final String uri, final String statusTName, final String   statusQName, StatusBean bean) {
		this.uri           = uri;
		this.statusTName     = statusTName;
		this.statusQName   = statusQName;
		this.bean          = bean;
		
		bean.setStatus(Status.QUEUED);
		broadcast(bean);
	}
	
	
	/**
	 * @return true if windows
	 */
	static public final boolean isWindowsOS() {
		return (System.getProperty("os.name").indexOf("Windows") == 0);
	}

	/**
	 * Writes the project bean at the point where it is run.
	 * 
	 * @param processingDir2
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	protected void writeProjectBean(String dir) throws Exception {
		
		final File beanFile = new File(dir, "projectBean.json");
    	ObjectMapper mapper = new ObjectMapper();
    	
    	final FileOutputStream stream = new FileOutputStream(beanFile);
    	try {
    	    mapper.writeValue(stream, bean);
    	} finally {
    		stream.close();
    	}
	}


	/**
	 * Call to start the process and broadcast status
	 * updates. Subclasses may redefine what is done
	 * on the start method, by default a thread is started
	 * in daemon mode to run things.
	 */
	public void start() {
		
		final Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}

	/**
	 * Notify any clients of the beans status
	 * @param bean
	 */
	protected void broadcast(StatusBean tbean) {
		try {
			bean.merge(tbean);
			cancelMonitor();
			updateQueue(bean); // For clients connecting in future or after a refresh - persistence.
			sendTopic(bean);   // For topic listeners wait for updates (more efficient than polling queue)
		} catch (Exception e) {
			e.printStackTrace();
		}
 	}

	/**
	 * Cancels the current topic monitor, if there is one. Prints exception if cannot.
	 */
	private void cancelMonitor() {
		if (bean.getStatus().isFinal() && topicConnection!=null) {
			try {
			    topicConnection.close();
			} catch (Exception ne) {
				ne.printStackTrace();
			}
		}
	}

	protected Connection topicConnection;

	/**
	 * Starts a thread which listens to the topic and if
	 * a cancel is found published, tries to terminate the subprocess.
	 * 
	 * @param p
	 */
    protected void startTerminateMonitor(final Process p, final String dir) {
		

    	final Thread cancelMonitor = new Thread(new Runnable() {
    		public void run() {
    			
    			try {
					ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
					ProgressableProcess.this.topicConnection = connectionFactory.createConnection();
			        topicConnection.start();
	
			        Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	
			        final Topic           topic    = session.createTopic(statusTName);
			        final MessageConsumer consumer = session.createConsumer(topic);
	
			        final Class        clazz  = bean.getClass();
			        final ObjectMapper mapper = new ObjectMapper();
			        
			        MessageListener listener = new MessageListener() {
			            public void onMessage(Message message) {		            	
			                try {
			                    if (message instanceof TextMessage) {
			                        TextMessage t = (TextMessage) message;
			        				final StatusBean tbean = mapper.readValue(t.getText(), clazz);
	                                
			        				if (bean.getUniqueId().equals(tbean.getUniqueId())) {
				        				if (tbean.getStatus() == Status.REQUEST_TERMINATE) {
				        					bean.merge(tbean);
				        					System.out.println("Terminating job '"+tbean.getName()+"'");
				        					terminate(p, dir);
				        					p.destroy();
				        				}
			        				}
			        				
			                    }
			                } catch (Exception e) {
			                    e.printStackTrace();
			                }
			            }

			        };
			        consumer.setMessageListener(listener);
			        
    			} catch (Exception ne) {
    				
    				ne.printStackTrace();
    			}

    		}
    	});
    	cancelMonitor.setDaemon(true);
    	cancelMonitor.setPriority(Thread.MIN_PRIORITY);
    	cancelMonitor.setName("Monitor for cancellation of '"+bean.getName()+"'");
    	cancelMonitor.start();
    }
    
    /**
     * Forcibly kills a process tree.
     * @param p
     * @throws Exception
     */
	private void terminate(Process process, final String dir) throws Exception {

	    final int pid = getPid(process);
	    
	    if (Platform.isWindows()) {
	    	
	        // Not sure if this works
	        POSIX.INSTANCE.kill(pid, 9);
	        
	    } else if (Platform.isLinux()) {
	    	
	    	// Use pkill, seems to kill all of the tree more reliably
	    	ProcessBuilder pb = new ProcessBuilder();
			
			// Can adjust env if needed:
			// Map<String, String> env = pb.environment();
			pb.directory(new File(dir));
			
			File log = new File(dir, "xia2_kill.txt");
			pb.redirectErrorStream(true);
			pb.redirectOutput(Redirect.appendTo(log));
			
			pb.command("bash", "-c", "pkill -9 -s "+pid);
			
			Process p = pb.start();
			p.waitFor();
	    }
	    
	    bean.setStatus(Status.CANCELLED);
	    bean.setMessage("Foricibly terminated before finishing.");
		broadcast(bean);
	}

	public static int getPid(Process p) throws Exception {
		
		if (Platform.isWindows()) {
			Field f = p.getClass().getDeclaredField("handle");
			f.setAccessible(true);
			int pid = Kernel32.INSTANCE.GetProcessId((Long) f.get(p));
			return pid;
			
		} else if (Platform.isLinux()) {
			Field	f = p.getClass().getDeclaredField("pid");
			f.setAccessible(true);
			int pid = (Integer) f.get(p);
			return pid;

		} else{
			throw new Exception("Cannot currently process pid for "+System.getProperty("os.name"));
		}
	}


	/**
	 * 
	 * @param bean
	 * @throws Exception 
	 */
	private void updateQueue(StatusBean bean) throws Exception {
		
		QueueConnection qCon = null;
		
		try {
	 	    QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			qCon  = connectionFactory.createQueueConnection(); 
			QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue   = qSes.createQueue(statusQName);
			qCon.start();
			
		    QueueBrowser qb = qSes.createBrowser(queue);
		    
		    @SuppressWarnings("rawtypes")
			Enumeration  e  = qb.getEnumeration();
		    
			ObjectMapper mapper = new ObjectMapper();
			String jMSMessageID = null;
	        while(e.hasMoreElements()) {
		    	Message m = (Message)e.nextElement();
		    	if (m==null) continue;
	        	if (m instanceof TextMessage) {
	            	TextMessage t = (TextMessage)m;
	              	
	            	@SuppressWarnings("unchecked")
					final StatusBean qbean = mapper.readValue(t.getText(), bean.getClass());
	            	if (qbean==null)               continue;
	            	if (qbean.getUniqueId()==null) continue; // Definitely not our bean
	            	if (qbean.getUniqueId().equals(bean.getUniqueId())) {
	            		jMSMessageID = t.getJMSMessageID();
	            		break;
	            	}
	        	}
		    }
	        
	        if (jMSMessageID!=null) {
	        	MessageConsumer consumer = qSes.createConsumer(queue, "JMSMessageID = '"+jMSMessageID+"'");
	        	Message m = consumer.receive(1000);
	        	if (m!=null && m instanceof TextMessage) {
	        		MessageProducer producer = qSes.createProducer(queue);
	        		producer.send(qSes.createTextMessage(mapper.writeValueAsString(bean)));
	        	}
	        }
		} finally {
			if (qCon!=null) qCon.close();
		}
		
	}

	private void sendTopic(StatusBean bean) throws Exception {
		JSONUtils.sendTopic(bean, statusTName, uri);
	}

	public boolean isCancelled() {
		return isCancelled;
	}


	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
	
	protected void dryRun() {
		for (int i = 0; i < 100; i++) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			bean.setPercentComplete(i);
			broadcast(bean);
		}

		bean.setStatus(Status.COMPLETE);
		bean.setPercentComplete(100);
		bean.setMessage("Dry run complete (no software run)");
		broadcast(bean);
	}

	


	/**
	 * @param dir
	 * @param template
	 * @param ext
	 * @param i
	 * @return file
	 */
	protected final static File getUnique(final File dir, final String template, final String ext, int i) {
		final String extension = ext != null ? (ext.startsWith(".")) ? ext : "." + ext : null;
		final File file = ext != null ? new File(dir, template + i + extension) : new File(dir, template + i);
		if (!file.exists()) {
			return file;
		}

		return getUnique(dir, template, ext, ++i);
	}

}
