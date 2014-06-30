package org.dawnsci.commandserver.core.process;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.producer.Broadcaster;

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
	protected final URI        uri;
	protected final String     statusTName;
	protected final String     statusQName;
	private Broadcaster        broadcaster;

	public ProgressableProcess(final URI uri, final String statusTName, final String statusQName, StatusBean bean) {
		this.uri           = uri;
		this.statusTName   = statusTName;
		this.statusQName   = statusQName;
		this.bean          = bean;
		this.broadcaster   = new Broadcaster(uri, statusQName, statusTName);
		bean.setStatus(Status.QUEUED);
		try {
			bean.setHostName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			// Not fatal but would be nice...
			e.printStackTrace();
		}
		broadcast(bean);
	}
	
	@Override
	public final void run() {
        try {
        	execute();
        } catch (Exception ne) {
        	
			bean.setStatus(Status.FAILED);
			bean.setMessage(ne.getMessage());
			bean.setPercentComplete(0);
			broadcast(bean);
        }
	}
	
	/**
	 * Execute the process, if an exception is thrown the process is set to 
	 * failed and the message is the message of the exception.
	 * 
	 * @throws Exception
	 */
	protected abstract void execute() throws Exception;
	
	/**
	 * Please provide a termination for the process by implementing this method.
	 * If the process has a stop file, write it now; if it needs to be killed,
	 * get its pid and kill it; if it is running on a cluster, use the qdel or dramaa api.
	 * 
	 * @throws Exception
	 */
	protected abstract void terminate() throws Exception;
	
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
	 * @param fileName - name of file
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	protected void writeProjectBean(final String dir, final String fileName) throws Exception {
		
		final File beanFile = new File(dir, fileName);
    	ObjectMapper mapper = new ObjectMapper();
    	beanFile.getParentFile().mkdirs();
    	if (!beanFile.exists()) beanFile.createNewFile();
    	
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
			broadcaster.broadcast(bean);
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
	 * Starts a connection which listens to the topic and if
	 * a cancel is found published, tries to terminate the subprocess.
	 * 
	 * @param p
	 */
    protected void createTerminateListener() throws Exception {
		
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

    					if (bean.getStatus().isFinal()) { // Something else already happened
    						topicConnection.close();
    						return;
    					}

    					if (bean.getUniqueId().equals(tbean.getUniqueId())) {
    						if (tbean.getStatus() == Status.REQUEST_TERMINATE) {
    							bean.merge(tbean);
    							System.out.println("Terminating job '"+tbean.getName()+"'");

    							terminate();
    							topicConnection.close();

    							bean.setStatus(Status.TERMINATED);
    							bean.setMessage("Foricibly terminated before finishing.");
    							broadcast(bean);

    							return;
    						}
    					}

    				}
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}

    	};
    	consumer.setMessageListener(listener);

    }

    protected void pkill(int pid, String dir) throws Exception {
    	
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

	protected static int getPid(Process p) throws Exception {
		
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


	public boolean isCancelled() {
		return isCancelled;
	}


	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
	
	protected void dryRun() {
		for (int i = 0; i < 100; i++) {
			
			if (bean.getStatus()==Status.REQUEST_TERMINATE ||
			    bean.getStatus()==Status.TERMINATED) {
				return;
			}
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
