package org.dawnsci.commandserver.jython;

import java.net.URI;

import org.dawnsci.commandserver.core.consumer.RemoteSubmission;

/**
 * Class to test that we can run 
 * 
 * @author fcp94556
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

		final RemoteSubmission factory = new RemoteSubmission(uri);
		factory.setQueueName("scisoft.jython.SUBMISSION_QUEUE");
		
		factory.submit(jbean, true);

	}
}
