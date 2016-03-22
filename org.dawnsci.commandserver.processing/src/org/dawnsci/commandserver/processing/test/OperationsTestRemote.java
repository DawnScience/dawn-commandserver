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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.dawnsci.commandserver.processing.OperationSubmission;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IFunction;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.ExecutionType;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.analysis.api.processing.OperationData;
import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.impl.Random;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.dawnsci.hdf.object.HierarchicalDataFactory;
import org.eclipse.dawnsci.hdf.object.IHierarchicalDataFile;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import uk.ac.diamond.scisoft.analysis.fitting.functions.FunctionFactory;
import uk.ac.diamond.scisoft.analysis.processing.operations.FunctionModel;
import uk.ac.diamond.scisoft.analysis.processing.operations.SectorIntegrationModel;
import uk.ac.diamond.scisoft.analysis.processing.operations.ValueModel;

import com.fasterxml.jackson.databind.ObjectMapper;

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
	private static IPersistenceService pservice;
	
	// OSGI fills this for us
	public static void setOperationService(IOperationService s) {
		oservice = s;
	}
	
	public static void setPersistenceService(IPersistenceService p) {
		pservice = p;
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
//			context.setSlicing("all"); // The 10 in the first dimension.
			context.setDataDimensions(new int[]{1,2});
			
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
		OperationBean obean = factory.submit(context2);

		// Blocks until a final state is reached
		Thread.sleep(2000); 
		factory.setQueueName("scisoft.operation.STATUS_QUEUE");
		final StatusBean bean = monitor(obean, factory.getUri(), factory.getQueueName());

		if (bean.getStatus()!=Status.COMPLETE) throw new Exception("Remote run failed! "+bean.getMessage());
		System.out.println(bean);		
	}
	
	@Ignore
	@Test
	public void testRemoteReal() throws Exception {
		IPersistentFile pf = pservice.getPersistentFile("/dls/science/groups/das/ExampleData/powder/NiceExamples/I12/temperature/39669_processed_150331_164949.nxs");
		IOperation<? extends IOperationModel, ? extends OperationData>[] ops = pf.getOperations();
		pf.close();
		IOperationContext con = oservice.createContext();
		con.setSeries(ops);
		con.setFilePath("/dls/science/groups/das/ExampleData/powder/NiceExamples/I12/temperature/39669.nxs");
		con.setDatasetPath("/entry1/pixium10_tif/image_data");
//		con.setSlicing("all");
		context.setDataDimensions(new int[]{1,2});
		Map<Integer, String> axesNames = new HashMap<Integer, String>();
		axesNames.put(1, "/entry1/pixium10_tif/linkamTemp");
		
		testRemoteRun(con);
		
	}
	
	@Ignore
	@Test
	public void testRemoteRealBig() throws Exception {
		IPersistentFile pf = pservice.getPersistentFile("/dls/science/groups/das/ExampleData/powder/NiceExamples/I12/big/42016_processed_150409_163751.nxs");
		IOperation<? extends IOperationModel, ? extends OperationData>[] ops = pf.getOperations();
		pf.close();
		IOperationContext con = oservice.createContext();
		con.setSeries(ops);
		con.setFilePath("/dls/science/groups/das/ExampleData/powder/NiceExamples/I12/big/42016.nxs");
		con.setDatasetPath("/entry1/instrument/pixium10_tif/image_data");
//		con.setSlicing("0:20");
		context.setDataDimensions(new int[]{1,2});

//		Map<Integer, String> axesNames = new HashMap<Integer, String>();
//		axesNames.put(1, "/entry1/pixium10_tif/linkamTemp");
		
		testRemoteRun(con);
		
	}
	
	private void testRemoteRunBean(OperationBean obean) throws Exception {
		// Run the model
		OperationSubmission factory = new OperationSubmission(new URI("tcp://sci-serv5.diamond.ac.uk:61616"));
		factory.prepare(obean);
		factory.directSubmit(obean);

		// Blocks until a final state is reached
		Thread.sleep(2000); 
		factory.setQueueName("scisoft.operation.STATUS_QUEUE");
		final StatusBean bean = monitor(obean, factory.getUri(), factory.getQueueName());

		if (bean.getStatus()!=Status.COMPLETE) throw new Exception("Remote run failed! "+bean.getMessage());
		System.out.println(bean);		
	}
	
	@Ignore
	@Test
	public void testRemoteRealBean() throws Exception {
		OperationBean b = new OperationBean();
		
		b.setDeletePersistenceFile(false);
		b.setPersistencePath("/dls/science/groups/das/ExampleData/powder/NiceExamples/I12/temperature/39669_processed_150507_134350.nxs");
		b.setFilePath("/dls/science/groups/das/ExampleData/powder/NiceExamples/I12/temperature/39669.nxs");
		b.setDatasetPath("/entry1/pixium10_tif/image_data");
		Map<Integer,String> s = new HashMap<Integer,String>();
		s.put(0, "all");
//		b.setSlicing(s);
		context.setDataDimensions(new int[]{1,2});
		Map<Integer, String> axesNames = new HashMap<Integer, String>();
		axesNames.put(1, "/entry1/pixium10_tif/linkamTemp");
		b.setAxesNames(axesNames);
		b.setOutputFilePath("/dls/science/groups/das/ExampleData/powder/remotetest/output3.nxs");
		testRemoteRunBean(b);
	}
	
	@Ignore
	@Test
	public void testRemoteRealBeanCluster() throws Exception {
		OperationBean b = new OperationBean();
		b.setRunDirectory("/dls/tmp/operations/");
		b.setDeletePersistenceFile(false);
		b.setPersistencePath("/dls/i12/data/2015/cm12163-2/processing/Jacob/46923_processed_150623_135514.nxs");
		b.setFilePath("/dls/i12/data/2015/cm12163-2/rawdata/46923.nxs");
		b.setDatasetPath("/entry1/pixium10_tif/image_data");
		Map<Integer,String> s = new HashMap<Integer,String>();
		s.put(0, "all");
//		b.setSlicing(s);
		context.setDataDimensions(new int[]{1,2});
		b.setXmx("4096m");
		b.setExecutionType(ExecutionType.PARALLEL);
		Map<Integer, String> axesNames = new HashMap<Integer, String>();
		axesNames.put(1, "/entry1/pixium10_tif/linkamTemp");
		b.setAxesNames(axesNames);
		b.setOutputFilePath("/dls/science/groups/das/ExampleData/tmp/output_cluster_0.nxs");
		
		// Run the model
		OperationSubmission factory = new OperationSubmission(new URI("tcp://sci-serv5.diamond.ac.uk:61616"),b.getRunDirectory());
		factory.prepare(b);
		factory.directSubmit(b);

		// Blocks until a final state is reached
		Thread.sleep(2000); 
		factory.setQueueName("scisoft.operation.STATUS_QUEUE");
		final StatusBean bean = monitor(b, factory.getUri(), factory.getQueueName());

		if (bean.getStatus()!=Status.COMPLETE) throw new Exception("Remote run failed! "+bean.getMessage());
		System.out.println(bean);	
	}

	
	/**
	 * Monitors a given bean in the status queue. 
	 * If the bean is not there throws exception.
	 * If the bean is in a final state, returns the bean straight away.
	 * 
	 * Polls the queue for the unique id of the bean we want until it
	 * encounters a final state of that bean.
	 * 
	 * Polling rate is less than 1s
	 * 
	 * NOTE this class can poll forever if the job it is looking at never finishes.
	 * 
	 * @param obean
	 * @param string
	 * @return the bean once it is in a final state.
	 * @throws exception if broker or queue absent
	 */
	public StatusBean monitor(StatusBean obean, URI uri, String queueName) throws Exception {
		
		if (queueName==null || "".equals(queueName)) throw new Exception("Please specify a queue name!");
		
		QueueConnectionFactory connectionFactory = (QueueConnectionFactory)ActiveMQServiceHolder.getEventConnectorService().createConnectionFactory(uri);
		QueueConnection qCon  = connectionFactory.createQueueConnection(); // This times out when the server is not there.
		QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue   = qSes.createQueue(queueName);
		qCon.start();
		
    	Class<? extends StatusBean> clazz = obean.getClass();
    	ObjectMapper mapper = new ObjectMapper();
	   
    	try {
	    	POLL: while(true) {
	    		
	    		Thread.sleep(500);
	    		QueueBrowser qb = qSes.createBrowser(queue);
		    	@SuppressWarnings("rawtypes")
		    	Enumeration  e  = qb.getEnumeration();
	
		    	while(e.hasMoreElements()) { // We must final the bean somewhere.
		    		Message m = (Message)e.nextElement();
		    		if (m==null) continue;
		    		if (m instanceof TextMessage) {
		    			TextMessage t = (TextMessage)m;
		    			final StatusBean bean = mapper.readValue(t.getText(), clazz);

		    			if (bean.getUniqueId().equals(obean.getUniqueId())) {
		    				if (bean.getStatus().isFinal()) return bean;
		    				continue POLL;
		    			}
		    		}
		    	}
		    	
		    	throw new Exception("The bean with id "+obean.getUniqueId()+" does not exist in "+queueName+"!");
	
		    }
    	} finally {
    		qCon.close();
    	}
	}

}
