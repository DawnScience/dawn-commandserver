/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.processing;

import java.io.File;
import java.net.URI;

import org.dawnsci.commandserver.core.consumer.RemoteSubmission;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.analysis.dataset.impl.Random;
import org.eclipse.dawnsci.hdf5.HierarchicalDataFactory;
import org.eclipse.dawnsci.hdf5.IHierarchicalDataFile;
import org.junit.Test;

/**
 * Class to test that we can run an operation pipeline remotely.
 * 
 * Run this as a plugin test
 * 
 * @author Matthew Gerring
 *
 */
public class TestOperationRun {

	private static IOperationService    oservice;
	private static IPersistenceService  pservice;
	
	// OSGI fills this for us
	public static void setOperationService(IOperationService s) {
		oservice = s;
	}
	// Set by OSGI
	public static void setPersistenceService(IPersistenceService s) {
		pservice = s;
	}

	@Test
	public void executeOperation() throws Exception {
		
		URI uri = new URI("tcp://sci-serv5.diamond.ac.uk:61616");
		
		OperationBean obean = new OperationBean();
		obean.setName("Test operation pipeline");
		obean.setMessage("A test operation pipeline execution");
		obean.setRunDirectory("C:/tmp/operationPipelineTest");
		
		// Some random data, the real execution will have dataset path
		createSomeRandomData(obean);
		
		// A Pipeline
		IOperationContext context = oservice.createContext();
		// TODO FIXME
	
		final RemoteSubmission factory = new RemoteSubmission(uri);
		factory.setQueueName("scisoft.operation.SUBMISSION_QUEUE");
		
		factory.submit(obean, true);

	}

	private static void createSomeRandomData(OperationBean obean) throws Exception {
		

		final File output = new File(obean.getRunDirectory()+"/data.nxs");
		output.getParentFile().mkdirs();
		IHierarchicalDataFile file = HierarchicalDataFactory.getWriter(output.getAbsolutePath());
		try {
			final IDataset data = Random.rand(0.0, 10.0, 10, 128, 128);
			String group   = file.group("/entry/");
			String dataset = file.createDataset("data", data, group);
			
			obean.setFileName(file.getPath());
			obean.setDatasetPath(dataset);
			
		} finally {
			file.close();
		}
 	}
}
