/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.ccp4.commandserver.mrbump.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.process.ProgressableProcess;

import uk.ac.ccp4.commandserver.mrbump.beans.BumpBean;

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
public class BumpProcess extends ProgressableProcess{
	
	private static String SETUP_COMMAND = "module load ccp4"; // Change by setting org.dawnsci.commandserver.tomo.moduleCommand
	private static String BUMP_COMMAND  = "mrbump ... "; // TODO  Change by setting org.dawnsci.commandserver.tomo.reconstructionCommand

	private String processingDir;
	
	public BumpProcess(URI        uri, 
			           String     statusTName, 
			           String     statusQName,
			           BumpBean bean) {
		super(uri, statusTName, statusQName, bean);
		
        final String runDir;
		if (isWindowsOS()) {
			// We are likely to be a test consumer, anyway the unix paths
			// from ISPyB will certainly not work, so we process in C:/tmp/
			runDir  = "C:/tmp/"+bean.getRunDirectory();
		} else {
			runDir  = bean.getRunDirectory();
		}

 		final File   bumpDir = getUnique(new File(runDir), "MR_", 1);
		bumpDir.mkdirs();
		
	    processingDir = bumpDir.getAbsolutePath();
		bean.setRunDirectory(processingDir);
		
 		try {
			setLoggingFile(new File(bumpDir, "bumpJavaProcessLog.txt"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(processingDir, "xia2Bean.json");
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
		
		// TODO Is this even needed?
		//writeFile();
		
		// TODO Remove this, it is just to give an idea of how something can report progress to the UI.
		createTerminateListener();
		dryRun();
		
		// TODO Actually run something?
		// runReconstruction();
	}

	/**
	 * TODO Please implement the running of tomo properly
	 */
	protected void runReconstruction() throws Exception {
		
		ProcessBuilder pb = new ProcessBuilder();
		
		// Can adjust env if needed:
		// Map<String, String> env = pb.environment();
		pb.directory(new File(processingDir));
		
		File log = new File(processingDir, "reconstruction_output.txt");
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		
		if (isWindowsOS()) {
		    pb.command("cmd", "/C", createTomoCommand());
		} else {
		    pb.command("bash", "-c", createTomoCommand());
		}

		Process p = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert p.getInputStream().read() == -1;	

		// Now we check if xia2 itself failed
		// In order to know this we look for a file with the extension .error with a 
		// String in it "Error:"
		// We assume that this failure happens fast during this sleep.
		Thread.sleep(1000);
		// checkTomoErrors(); // We do this to avoid starting an output file monitor at all.

		// Now we monitor the output file. Then we wait for the process, then we check for errors again.
		// startProgressMonitor();
		// createTerminateListener();
		p.waitFor();
		// checkXia2Errors(); // Check errors again at end					

		bean.setStatus(Status.COMPLETE);
		bean.setMessage("MR run completed normally");
		bean.setPercentComplete(100);
		broadcast(bean);


	}
	

	@Override
	public void terminate() throws Exception {
		// Please implement to clean up on the cluster.
	}

	private String createTomoCommand() {
		
		return "MrBump command TODO!";
	}

	private void writeFile() throws Exception {
		
        // TODO Does mrbump need a file to drive its running?
	}

	public String getProcessingDir() {
		return processingDir;
	}

	public void setProcessingDir(String processingDir) {
		this.processingDir = processingDir;
	}

}
