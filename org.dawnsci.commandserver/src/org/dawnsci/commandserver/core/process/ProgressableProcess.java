/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.core.process;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Platform;


/**
 * Extend to provide a connection between a running process.
 * 
 * This class has a user readable log file, represented by PrintStream out
 * and a logger. A given message might be applicable for both places, depending
 * on what message the user might need to see.
 * 
 * @author Matthew Gerring
 *
 */
public abstract class ProgressableProcess implements Runnable, IBroadcaster {

	private static final Logger logger = LoggerFactory.getLogger(ProgressableProcess.class);


	private boolean            blocking    = false;
	private boolean            isCancelled = false;
	protected StatusBean       bean;
	protected URI              uri;
	protected String           statusTName;
	protected String           statusQName;
	private Broadcaster        broadcaster;
	
	protected PrintStream out = System.out;

	protected ProgressableProcess() {
		super();
	}
	
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
			logger.warn("Cannot find local host!", e);
		}
		broadcast(bean);
	}
	
	protected void setLoggingFile(File logFile) throws IOException {
		setLoggingFile(logFile, false);
	}
	/**
	 * Calling this method redirects the logging of this Java object
	 * which is available through the field 'out' to a known file.
	 * 
	 * @param logFile
	 * @throws IOException 
	 */
	protected void setLoggingFile(File logFile, boolean append) throws IOException {
		if (!logFile.exists()) logFile.createNewFile();
		this.out = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, append)), true, "UTF-8");
		broadcaster.setLoggingStream(out);
	}
	
	@Override
	public final void run() {
        try {
        	execute();
        	if (out!=System.out) {
        		out.close();
        		out = System.out;
        	}
        } catch (Exception ne) {
        	ne.printStackTrace(out);
			logger.error("Cannot run process!", ne);
        	
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
	public abstract void execute() throws Exception;
	
	/**
	 * Please provide a termination for the process by implementing this method.
	 * If the process has a stop file, write it now; if it needs to be killed,
	 * get its pid and kill it; if it is running on a cluster, use the qdel or dramaa api.
	 * 
	 * @throws Exception
	 */
	public abstract void terminate() throws Exception;
	
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
		
		writeProjectBean(new File(dir), fileName);
	}
	
	/**
	 * 
	 * @param dir
	 * @param fileName
	 * @throws Exception
	 */
	protected void writeProjectBean(final File dir, final String fileName) throws Exception {
		
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
		
		if (isBlocking()) {
			run(); // Block until process has run.
		} else {
			final Thread thread = new Thread(this);
			thread.setDaemon(true);
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
		}
	}

	/**
	 * Notify any clients of the beans status
	 * @param bean
	 */
	@Override
	public void broadcast(StatusBean tbean) {
		try {
			bean.merge(tbean);
			cancelMonitor();
			broadcaster.broadcast(bean, false);
		} catch (Exception e) {
			logger.error("Cannot broadcast", e);
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
				logger.error("Cannot close topic", ne);
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

    	final Class<? extends StatusBean> clazz = bean.getClass();
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
    							out.println("Terminating job '"+tbean.getName()+"'");

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
    				logger.error("Cannot deal with message "+message, e);
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
		dryRun(100);
	}
	protected void dryRun(int size) {
        dryRun(size, true);
	}
	
	protected void dryRun(int size, boolean complete) {
		for (int i = 0; i < size; i++) {
			
			if (bean.getStatus()==Status.REQUEST_TERMINATE ||
			    bean.getStatus()==Status.TERMINATED) {
				return;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.error("Dry run sleeping failed", e);
			}
			System.out.println("Dry run : "+bean.getPercentComplete());
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
	protected final static File getUnique(final File dir, final String template, int i) {
		
		final File file = new File(dir, template + i );
		if (!file.exists()) {
			return file;
		}

		return getUnique(dir, template, ++i);
	}

	public boolean isBlocking() {
		return blocking;
	}

	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	
	
	public static final String getLegalFileName(String name) {
		name = name.replace(" ", "_");
		name = name.replaceAll("[^a-zA-Z0-9_]", "");
        return name;
	}


}
