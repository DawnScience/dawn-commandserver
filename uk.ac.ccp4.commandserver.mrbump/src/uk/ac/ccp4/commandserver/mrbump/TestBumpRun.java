/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.ccp4.commandserver.mrbump;

import java.net.URI;

import org.dawnsci.commandserver.core.consumer.RemoteSubmission;

import uk.ac.ccp4.commandserver.mrbump.beans.BumpBean;

/**
 * Class to test that we can run 
 * 
 * @author Matthew Gerring
 *
 */
public class TestBumpRun {


	public static void main(String[] args) throws Exception {
		
		URI uri = new URI("tcp://sci-serv5.diamond.ac.uk:61616");
		
		BumpBean bean = new BumpBean();
		bean.setMtzFile(" mzt path ... ");
		bean.setSequenceFile(" sequence path...");

		final RemoteSubmission factory = new RemoteSubmission(uri);
		factory.setQueueName("scisoft.mrbump.SUBMISSION_QUEUE");
		
		factory.submit(bean, true);

	}
}
