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

import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.Status;
import org.python.util.PythonInterpreter;

//import uk.ac.diamond.scisoft.python.JythonInterpreterUtils;

public class JythonProcess extends ProgressableProcess<JythonBean> {

	private JythonBean          jybean;
	private PythonInterpreter jythonInterpreter;


	public JythonProcess(JythonBean bean, PythonInterpreter interpreter, IPublisher<JythonBean> status) {
		
		super(bean, status, false);
		
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

 		final File   jythonDir = getUnique(new File(runDir), "Run_", 1);
 		jythonDir.mkdirs();
 		
// 		try {
//			setLoggingFile(new File(jythonDir, "jythonProcessLog.txt"), true);
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
				
		bean.setRunDirectory(jythonDir.getAbsolutePath());
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(jythonDir, "jythonRun.json");
		} catch (Exception e) {
			e.printStackTrace(out);
		}

	}

	@Override
	public void execute() throws EventException {
		
		jybean.setStatus(Status.RUNNING);
		jybean.setPercentComplete(1);
		broadcast(jybean);
		
//		out.println(System.getProperty("java.class.path"));
//		out.println(System.getProperty("java.home"));
	
		//Check whether we're running a script (should be in production!) or raw code
		if (jybean.getRunScript() == true) {
			String jyScriptPath = new File(jybean.getJythonCode()).getAbsolutePath();
			jythonInterpreter.execfile(jyScriptPath);
			jythonInterpreter.exec("__dlsCleanup()");//Removes all added names from dicts.
		}
		else {
			jythonInterpreter.exec(jybean.getJythonCode());
			jythonInterpreter.exec("__dlsCleanup()");//Removes all added names from dicts.
		}
		
		//What happens if the user includes a exit() call to jython??? - Looks ok...
		//TODO Check interpreter is still alive before running, if not, call it up.

		jybean.setStatus(Status.COMPLETE);
		jybean.setMessage("Finished running Jython "+jybean.getJythonClass());
		jybean.setPercentComplete(100);
		broadcast(jybean);

	}

	@Override
	public void terminate() throws EventException {
		// TODO Auto-generated method stub
	    out.println("terminate Jython!");
	}

}
