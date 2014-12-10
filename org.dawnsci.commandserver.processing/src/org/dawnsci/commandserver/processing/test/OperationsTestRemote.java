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
import org.dawnsci.commandserver.processing.OperationSubmission;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IFunction;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.impl.Random;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.dawnsci.hdf5.HierarchicalDataFactory;
import org.eclipse.dawnsci.hdf5.IHierarchicalDataFile;
import org.junit.Before;
import org.junit.Test;

import uk.ac.diamond.scisoft.analysis.fitting.functions.FunctionFactory;
import uk.ac.diamond.scisoft.analysis.processing.operations.FunctionModel;
import uk.ac.diamond.scisoft.analysis.processing.operations.SectorIntegrationModel;
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
	
	// OSGI fills this for us
	public static void setOperationService(IOperationService s) {
		oservice = s;
	}

	private IOperationContext   context;
	
	@Before
	public void setup() throws Exception {
				
		context = oservice.createContext();
		
		// Some random data, the real execution will have dataset path
		// Make some test data manually.
		final File output = new File((new OperationSubmission()).getRunDirectory()+"/data.nxs");
		output.getParentFile().mkdirs();
		if (output.exists()) output.delete();
		
		IHierarchicalDataFile file = HierarchicalDataFactory.getWriter(output.getAbsolutePath());
		try {
			final IDataset data = Random.rand(0.0, 10.0, 10, 128, 128);
			String group   = file.group("/entry/signal");
			String dataset = file.createDataset("data", data, group);
			
			context.setFilePath(file.getPath());
			context.setDatasetPath(dataset);
			context.setSlicing("all"); // The 10 in the first dimension.
			
		} finally {
			file.close();
		}
	}

	@Test
	public void testSimpleAddAndSubtractUsingFind() throws Exception {
		
        // Now we create a test array of operations and submit their data.
		final IOperation subtract = oservice.findFirst("subtractOperation");
		final IOperation add      = oservice.findFirst("add");
			
		subtract.setModel(new ValueModel(100));
		add.setModel(new ValueModel(101));
		
		context.setSeries(subtract, add);	
		testRemoteRun(context);
	}
	
	@Test
	public void testAzimuthalSimple() throws Exception {
		
		final IROI         sector = new SectorROI(10.0, 50.0, 20.0, 30.0,  Math.toRadians(90.0), Math.toRadians(180.0));
				
		final IOperation azi = oservice.findFirst("uk.ac.diamond.scisoft.analysis.processing.operations.azimuthalIntegration");
		azi.setModel(new SectorIntegrationModel(sector));

		context.setSeries(azi);	
		testRemoteRun(context);
	}
	
	@Test
	public void testFunctionSimple() throws Exception {
		
		final IOperation functionOp = oservice.findFirst("function");
		
		// y(x) = a_0 x^n + a_1 x^(n-1) + a_2 x^(n-2) + ... + a_(n-1) x + a_n
		final IFunction poly = FunctionFactory.getFunction("Polynomial", 3/*x^2*/, 5.3/*x*/, 9.4/*m*/);
		functionOp.setModel(new FunctionModel(poly));

		context.setSeries(functionOp);	
		testRemoteRun(context);
	}

	
	private void testRemoteRun(IOperationContext context2) throws Exception {
		// Run the model
		OperationSubmission factory = new OperationSubmission(new URI("tcp://sci-serv5.diamond.ac.uk:61616"));
		OperationBean obean = factory.submit(context);

		// Blocks until a final state is reached
		Thread.sleep(2000); 
		factory.setQueueName("scisoft.operation.STATUS_QUEUE");
		final StatusBean bean = factory.monitor(obean);

		if (bean.getStatus()!=Status.COMPLETE) throw new Exception("Remote run failed! "+bean.getMessage());
		System.out.println(bean);		
	}

}