/*
 * Copyright (c) 2014 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.ptycho.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.ptycho.beans.PtychoBean;

/**
 * Rerun of several collections as follows: o Write the Xia2 command file,
 * automatic.xinfo o Runs Xia2 with file o Progress reported by stating xia2.txt
 * o Runs xia2 html to generate report.
 * 
 * 
 */
public class PtychoProcess extends ProgressableProcess {

	private static String SETUP_COMMAND = "module load python"; // Change by
																// setting
																// org.dawnsci.commandserver.tomo.moduleCommand
	private static String PTYCHO_COMMAND = "ptycho ... "; // TODO Change by
															// setting
															// org.dawnsci.commandserver.tomo.reconstructionCommand
	private static String PTYCHO_FILE = "ptycho.txt"; // Change by setting
														// org.dawnsci.commandserver.tomo.reconstructionFile

	private String processingDir;

	public PtychoProcess(URI uri, String statusTName, String statusQName,
			PtychoBean bean) {
		super(uri, statusTName, statusQName, bean);

		final String runDir;
		if (isWindowsOS()) {
			// We are likely to be a test consumer, anyway the unix paths
			// from ISPyB will certainly not work, so we process in C:/tmp/
			runDir = "C:/tmp/" + bean.getRunDirectory();
		} else {
			runDir = bean.getRunDirectory();
		}

		final File ptychoDir = getUnique(new File(runDir), "Reconstruction_", 1);
		ptychoDir.mkdirs();

		processingDir = ptychoDir.getAbsolutePath();
		bean.setRunDirectory(processingDir);

		try {
			setLoggingFile(new File(ptychoDir, "ptychoJavaProcessLog.txt"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(processingDir, "ptychoBean.json");
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
		writeFile();

		// TODO Remove this, it is just to give an idea of how something can
		// report progress to the UI.
		createTerminateListener();
		dryRun();

		// TODO Actually run something?
		// runReconstruction();
	}

	/**
	 * TODO Please implement the running of ptycho properly
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
		// In order to know this we look for a file with the extension .error
		// with a
		// String in it "Error:"
		// We assume that this failure happens fast during this sleep.
		Thread.sleep(1000);

		// Now we monitor the output file. Then we wait for the process, then we
		// check for errors again.
		// startProgressMonitor();
		// createTerminateListener();
		p.waitFor();

		bean.setStatus(Status.COMPLETE);
		bean.setMessage("Reconstruction run completed normally");
		bean.setPercentComplete(100);
		broadcast(bean);

	}

	@Override
	public void terminate() throws Exception {
		// Please implement to clean up on the cluster.
	}

	private String createTomoCommand() {

		String setupCmd = "";
		if (!isWindowsOS()) { // We use module load xia2
			// Get a linux enviroment
			setupCmd = System
					.getProperty("org.dawnsci.commandserver.ptycho.moduleCommand") != null ? System
					.getProperty("org.dawnsci.commandserver.ptycho.moduleCommand")
					: SETUP_COMMAND;

			setupCmd += " ; ";
		}

		// For windows xia2 must be on the path already.
		String ptychoCmd = System
				.getProperty("org.dawnsci.commandserver.ptycho.reconstructionCommand") != null ? System
				.getProperty("org.dawnsci.commandserver.ptycho.reconstructionCommand")
				: PTYCHO_COMMAND;

		return setupCmd + ptychoCmd;
	}

	private void writeFile() throws Exception {

		// TODO Does ptycho need a file to drive its running?
	}

	public String getProcessingDir() {
		return processingDir;
	}

	public void setProcessingDir(String processingDir) {
		this.processingDir = processingDir;
	}

}
