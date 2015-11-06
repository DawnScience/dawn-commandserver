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
import java.net.UnknownHostException;
import java.util.Map;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.core.IConsumerProcess;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public abstract class ProgressableProcess<T extends StatusBean> implements Runnable, IConsumerProcess<T> {

	private static final Logger logger = LoggerFactory.getLogger(ProgressableProcess.class);


	private boolean            blocking    = false;
	private boolean            isCancelled = false;
	protected T       bean;
	private IPublisher<T> statusPublisher;
	protected Map<String, String> arguments;
	
	protected PrintStream out = System.out;

	protected ProgressableProcess() {
		super();
	}
	
	public ProgressableProcess(T bean, IPublisher<T> statusPublisher, boolean blocking) {
		
		this.bean            = bean;
		this.statusPublisher = statusPublisher;
		this.blocking        = blocking;
		
		bean.setStatus(Status.QUEUED);
		try {
			bean.setHostName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			logger.warn("Cannot find local host!", e);
		}
		broadcast(bean);
	}
	

	@Override
	public T getBean() {
		return bean;
	}


	@Override
	public IPublisher<T> getPublisher() {
		return statusPublisher;
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
		statusPublisher.setLoggingStream(out);
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
	public abstract void execute() throws EventException;
	
	/**
	 * Please provide a termination for the process by implementing this method.
	 * If the process has a stop file, write it now; if it needs to be killed,
	 * get its pid and kill it; if it is running on a cluster, use the qdel or dramaa api.
	 * 
	 * @throws Exception
	 */
	public abstract void terminate() throws EventException;
	
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
        final IEventConnectorService service = ActiveMQServiceHolder.getEventConnectorService();
    	beanFile.getParentFile().mkdirs();
    	if (!beanFile.exists()) beanFile.createNewFile();
    	
    	final FileOutputStream stream = new FileOutputStream(beanFile);
    	try {
    		String json = service.marshal(stream);
    		stream.write(json.getBytes("UTF-8"));
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
	public void broadcast(StatusBean tbean) {
		try {
			bean.merge(tbean);
			statusPublisher.broadcast(bean);
		} catch (Exception e) {
			logger.error("Cannot broadcast", e);
		}
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
			
			if (isCancelled) {
				bean.setStatus(Status.TERMINATED);
				broadcast(bean);
				return;
			}
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

	public Map<String, String> getArguments() {
		return arguments;
	}

	public void setArguments(Map<String, String> arguments) {
		this.arguments = arguments;
	}


}
