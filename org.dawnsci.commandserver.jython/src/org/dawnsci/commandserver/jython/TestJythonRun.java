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

import org.dawnsci.commandserver.core.consumer.RemoteSubmission;

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

		final RemoteSubmission factory = new RemoteSubmission(uri);
		factory.setQueueName("scisoft.jython.SUBMISSION_QUEUE");
		
		factory.submit(jbean, true);

	}
}
