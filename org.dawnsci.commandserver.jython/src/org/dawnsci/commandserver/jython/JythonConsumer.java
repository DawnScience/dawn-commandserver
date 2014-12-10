/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.jython;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.python.util.PythonInterpreter;

import uk.ac.diamond.scisoft.python.JythonInterpreterUtils;

public class JythonConsumer extends ProcessConsumer {
	
	protected PythonInterpreter interpreter;

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return JythonBean.class;
	}
	
	@Override
	public void start() throws Exception {
		//This starts the interpreter for script submission (Borrowed from org.dawb.passerelle.actors.scripts.PythonScript)
		
		//Additional paths (needed for using Autoreduction script)
		final Set<String> extras = new HashSet<String>();
		extras.add("com.fasterxml.jackson.core");
		extras.add("org.dawnsci.persistence");
		
		//All set? Let's go!
		interpreter = JythonInterpreterUtils.getFullInterpreter(null, extras);
		
		//Get shared python functions and pass in as a stream.
		URL pySOURL = this.getClass().getResource("python_funcs.py");
		if (pySOURL == null) {
			throw new FileNotFoundException("python_funcs.py (resource not found)");
		}
		interpreter.execfile(pySOURL.openStream());

		System.out.println("Jython interpreter started.");
		
		//This after other setup as this just sits and sits and...
		super.start();
	}
	
	@Override
	public void stop() throws Exception {
		//Need to kill the interpreter when stopping the Consumer
		interpreter.eval("exit()");
		System.out.println("Jython interpreter stopped.");
		
		super.stop();
	}


	@Override
	protected ProgressableProcess createProcess(URI uri, String statusTName, String statusQName, StatusBean bean) throws Exception {
		return new JythonProcess(uri, statusTName, statusQName, config, (JythonBean)bean, interpreter);
	}

	private static final long TWO_DAYS = 48*60*60*1000; // ms
	
	@Override
	protected long getMaximumRunningAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumJythonRunningAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumJythonRunningAge"));
		}
		return TWO_DAYS;
	}
	
	private static final long A_WEEK = 7*24*60*60*1000; // ms
	
	@Override
	protected long getMaximumCompleteAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumJythonCompleteAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumJythonCompleteAge"));
		}
		return A_WEEK;
	}

	@Override
	public String getName() {
		return "Jython VM";
	}

}
