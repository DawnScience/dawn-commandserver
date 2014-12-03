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
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.monitor.IMonitor;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.IExecutionVisitor;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;

/**
 * Rerun of several collections as follows:
 * o Write the Xia2 command file, automatic.xinfo
 * o Runs Xia2 with file
 * o Progress reported by stating xia2.txt
 * o Runs xia2 html to generate report.
 * 
 * @author Matthew Gerring
 *
 */
public class OperationProcess extends ProgressableProcess{
	
	private static IOperationService   oservice;
	private static IPersistenceService pservice;
	private static ILoaderService      lservice;
	
	// Set by OSGI
	public static void setOperationService(IOperationService s) {
		oservice = s;
	}
	// Set by OSGI
	public static void setPersistenceService(IPersistenceService s) {
		pservice = s;
	}
	// Set by OSGI
	public static void setLoaderService(ILoaderService s) {
		lservice = s;
	}
	// Used so that a no-argument constructor exists but is not useful.
	public OperationProcess() {
		super();
	}

	
	
	private String              processingDir;
	
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
			runPipeline();
			
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

	private void runPipeline() throws Throwable {
		
		OperationBean obean = (OperationBean)bean;
		IPersistentFile file = pservice.createPersistentFile(obean.getPersistencePath());
		try {
			// We should get these back exactly as they were defined.
		    final IOperation[] ops = file.getOperations();
		    
		    // Create a context and run the pipeline
		    final IOperationContext context = oservice.createContext();
		    context.setSeries(ops);
		    context.setExecutionType(obean.getExecutionType());
		    context.setParallelTimeout(obean.getParallelTimeout());
		    
		    final IDataHolder holder = lservice.getData(obean.getFilePath(), new IMonitor.Stub());
		    final ILazyDataset lz    = holder.getLazyDataset(obean.getDatasetPath());
		    context.setData(lz);
		    context.setSlicing(obean.getSlicing());
		    
		    // We create a visitor which publishes information about what
		    // operation was completed.
		    final IExecutionVisitor visitor = new OperationVisitor(lz, obean, this);
		    context.setVisitor(visitor);
		    
		    oservice.execute(context);
		    
		} finally {
			file.close();
			
			if (obean.isDeletePersistenceFile()) {
			    final File persFile = new File(obean.getPersistencePath());
			    persFile.delete();
			}
		}

	}

	@Override
	public void terminate() throws Exception {
		// Please implement to clean up on the cluster.
	}

	public String getProcessingDir() {
		return processingDir;
	}

	public void setProcessingDir(String processingDir) {
		this.processingDir = processingDir;
	}

}
