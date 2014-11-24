/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.processing.test;

import java.io.File;
import java.net.URI;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.consumer.RemoteSubmission;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.analysis.dataset.impl.Random;
import org.eclipse.dawnsci.hdf5.HierarchicalDataFactory;
import org.eclipse.dawnsci.hdf5.IHierarchicalDataFile;
import org.junit.Before;
import org.junit.Test;

import uk.ac.diamond.scisoft.analysis.processing.operations.ValueModel;

/**
 * Class to test that we can run an operation pipeline remotely.
 * 
 * To Run:
 * 1. Make sure a consumer is started either using 
 *   a) module load consumer and consumer start operations or 
 *   b) starting the debug run CommandServer-OperationConsumer
 *   
 * 2. Edit the paths in 
 * 2. Run this test s a plugin test. It will submit to the 
 *    consumer some test pipelines.
 * 
 * @author Matthew Gerring
 *
 */
public class OperationsTestRemote {

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

	private OperationBean obean;
	
	@Before
	public void setup() throws Exception {
				
		// TODO Wrap bean in a class that extends RemoteSubmission
		// and recieves an operation context.
		obean = new OperationBean();
		obean.setName("Test operation pipeline");
		obean.setPipelineName(getClass().getSimpleName());
		obean.setMessage("A test operation pipeline execution");
		if (System.getProperty("os.name").toLowerCase().contains(("windows"))) {
		    obean.setRunDirectory("C:/tmp/operationPipelineTest");
		} else {
			obean.setRunDirectory("/scratch/operationPipelineTest");
		}
		
		// Some random data, the real execution will have dataset path
		createSomeRandomData(obean);
		
	}

	@Test
	public void testSimpleAddAndSubtractUsingFind() throws Exception {
		
		final File persFile = new File(obean.getRunDirectory()+"/pipeline.nxs");
		persFile.getParentFile().mkdirs();
		if (persFile.exists()) persFile.delete();
		
        // Now we create a test array of operations and submit their data.
		final IOperation add      = oservice.findFirst("add");
		final IOperation subtract = oservice.findFirst("subtractOperation");
			
		subtract.setModel(new ValueModel(100));
		add.setModel(new ValueModel(101));
		
		IPersistentFile file = pservice.createPersistentFile(persFile.getAbsolutePath());
		try {
		    file.setOperations(add, subtract);
		    obean.setPersistencePath(persFile.getAbsolutePath());
		} finally {
			file.close();
		}

		// 
		final RemoteSubmission factory = new RemoteSubmission(new URI("tcp://sci-serv5.diamond.ac.uk:61616"));
		factory.setQueueName("scisoft.operation.SUBMISSION_QUEUE");
		factory.submit(obean, true);

		// Blocks until a final state is reached
		Thread.sleep(100); 
		factory.setQueueName("scisoft.operation.STATUS_QUEUE");
		final StatusBean bean = factory.monitor(obean);
		
		if (bean.getStatus()!=Status.COMPLETE) throw new Exception("Remote run failed! "+bean.getMessage());
	}

	private static void createSomeRandomData(OperationBean obean) throws Exception {
		
		final File output = new File(obean.getRunDirectory()+"/data.nxs");
		output.getParentFile().mkdirs();
		if (output.exists()) output.delete();
		
		IHierarchicalDataFile file = HierarchicalDataFactory.getWriter(output.getAbsolutePath());
		try {
			final IDataset data = Random.rand(0.0, 10.0, 10, 128, 128);
			String group   = file.group("/entry/signal");
			String dataset = file.createDataset("data", data, group);
			
			obean.setFileName(file.getPath());
			obean.setDatasetPath(dataset);
			obean.setSlicing("all");
			
		} finally {
			file.close();
		}
 	}
}
