/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.jython;

import java.net.URI;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.ISubmitter;

/**
 * Class to test that we can run 
 * 
 * @author Matthew Gerring
 *
 */
public class TestJythonRun {


	public static void main(String[] args) throws Exception {
		
		URI uri = new URI("tcp://sci-serv5.diamond.ac.uk:61616");
		
		JythonBean jbean = new JythonBean();
		jbean.setName("Test Jython");
		jbean.setMessage("A test jython execution");
		jbean.setJythonClass("org.dawnsci.some.jython.Class");
		jbean.setRunDirectory("C:/tmp/");
		jbean.setJythonCode("/scratch/dawn-ws_git/dawn-commandserver.git/org.dawnsci.commandserver.jython/test_scripts/report_paths.py");

		IEventService service = ActiveMQServiceHolder.getEventService();
		final ISubmitter<JythonBean> queueSub = service.createSubmitter(uri, "scisoft.jython.SUBMISSION_QUEUE");
		
		queueSub.submit(jbean);
		
		JythonBean jbean2 = new JythonBean();
		jbean2.setName("Test Jython2");
		jbean2.setMessage("A test jython execution");
		jbean2.setJythonClass("org.dawnsci.some.jython.Class");
		jbean2.setRunDirectory("C:/tmp/");
		jbean2.setJythonCode("/scratch/dawn-ws_git/dawn-commandserver.git/org.dawnsci.commandserver.jython/test_scripts/later_run.py");
				
		queueSub.submit(jbean2);

	}
}
