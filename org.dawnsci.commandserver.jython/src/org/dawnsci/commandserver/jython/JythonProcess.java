/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.jython;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.python.util.PythonInterpreter;

//import uk.ac.diamond.scisoft.python.JythonInterpreterUtils;

public class JythonProcess extends ProgressableProcess {

	private Map<String, String> args;
	private JythonBean          jybean;
	private PythonInterpreter jythonInterpreter;


	public JythonProcess(URI uri, String statusTName, String statusQName, Map<String, String> args, JythonBean bean, PythonInterpreter interpreter) {
		
		super(uri, statusTName, statusQName, bean);
		
		this.args  = args;
		this.jybean = bean;
		this.jythonInterpreter = interpreter;
		
		// We only run one script at a time.
		setBlocking(true);
		
        final String runDir;
		if (isWindowsOS()) {
			// We are likely to be a test consumer, anyway the unix paths
			// from ISPyB will certainly not work, so we process in C:/tmp/
			runDir  = "C:/tmp/"+bean.getRunDirectory();
		} else {
			runDir  = bean.getRunDirectory();
		}

 		final File   jythonDir = getUnique(new File(runDir), "Run_", null, 1);
 		jythonDir.mkdirs();
 		
 		try {
			setLoggingFile(new File(jythonDir, "jythonProcessLog.txt"), true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
				
		bean.setRunDirectory(jythonDir.getAbsolutePath());
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(jythonDir, "jythonRun.json");
		} catch (Exception e) {
			e.printStackTrace(out);
		}

	}

	@Override
	public void execute() throws Exception {
		
		jybean.setStatus(Status.RUNNING);
		jybean.setPercentComplete(1);
		broadcast(jybean);
		
		//Check whether we're running a script (should be in production!) or raw code
		if (jybean.getRunScript() == true) {
			String jyScriptPath = new File(jybean.getJythonCode()).getAbsolutePath();
			jythonInterpreter.execfile(jyScriptPath);
		}
		else {
			jythonInterpreter.exec(jybean.getJythonCode());
		}
		
		//What happens if the user includes a exit() call to jython??? - Looks ok...
		//TODO Check interpreter is still alive before running, if not, call it up.

		jybean.setStatus(Status.COMPLETE);
		jybean.setMessage("Finished running Jython "+jybean.getJythonClass());
		jybean.setPercentComplete(100);
		broadcast(jybean);

	}

	@Override
	public void terminate() throws Exception {
		// TODO Auto-generated method stub
	    out.println("terminate Jython!");
	}

}
