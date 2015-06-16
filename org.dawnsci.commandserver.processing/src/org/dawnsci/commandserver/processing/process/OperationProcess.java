/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.processing.process;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs the OperationPipeline by executing a dawn command.
 * This command runs in a separate process which can include
 * a cluster command if required.
 * 
 * @author Matthew Gerring
 *
 */
public class OperationProcess extends ProgressableProcess{
	
	private static final Logger logger = LoggerFactory.getLogger(OperationProcess.class);
	
	
	private String   processingDir;
    private Process  process;

	
	/**
	 * Used to run a process without a beam and for OSGi to inject
	 * services as required.
	 */
	public OperationProcess() {
		super();
	}

	public OperationProcess(URI        uri, 
			                String     statusTName, 
			                String     statusQName,
			                OperationBean bean) {
		
		super(uri, statusTName, statusQName, bean);
		
		setBlocking(false);
		
        final String runDir;
		if (isWindowsOS()) {
			// We are likely to be a test consumer, anyway the unix paths
			// from ISPyB will certainly not work, so we process in C:/tmp/
			runDir  = bean.getRunDirectory();
		} else {
			runDir  = bean.getRunDirectory();
		}

 		final File   dir = getUnique(new File(runDir), getLegalFileName(bean.getName())+"_", 1);
 		dir.mkdirs();
		
	    processingDir = dir.getAbsolutePath();
		bean.setRunDirectory(processingDir);
		
 		try {
			setLoggingFile(new File(dir, "operationProcessLog.txt"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(processingDir, "operationBean.json");
		} catch (Exception e) {
			e.printStackTrace(out);
		}
	}

	@Override
	public void execute() throws Exception {
		
		// Right we a starting the reconstruction, tell them.
		bean.setStatus(Status.RUNNING);
		bean.setPercentComplete(0d);
		broadcast(bean);
		
		createTerminateListener();
		
		try {
			// TODO Run as process out of DAWN similar to how workflows run
			File path = new File(processingDir, "operationBean.json");
			if (!path.exists()) throw new Exception("Cannot find path to OperationBean!");
			
			execute(path);
		
			// TODO Actually run something?
			bean.setStatus(Status.COMPLETE);
			bean.setMessage(((OperationBean)bean).getName()+" completed normally");
			bean.setPercentComplete(100);
			broadcast(bean);
			
		} catch (Throwable ne) {
			ne.printStackTrace();
			bean.setStatus(Status.FAILED);
			bean.setMessage(ne.getMessage());
			bean.setPercentComplete(0);
			broadcast(bean);
		}
	}

	private void execute(File path) {
		
		final String line = createExecutionLine(path);
		logger.debug("Execution line: "+line);
		
	}

	private String createExecutionLine(File path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void terminate() throws Exception {
		if (process!=null) process.destroyForcibly();
	}

	public String getProcessingDir() {
		return processingDir;
	}

	public void setProcessingDir(String processingDir) {
		this.processingDir = processingDir;
	}

}
