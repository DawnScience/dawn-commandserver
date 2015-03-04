package org.dawnsci.commandserver.tomo;

import java.net.URI;

import org.dawnsci.commandserver.core.consumer.RemoteSubmission;
import org.dawnsci.commandserver.tomo.beans.TomoBean;

public class TomoClient {

	
	public static void main(String[] args) throws Exception {
		
		URI uri = new URI("tcp://sci-serv5.diamond.ac.uk:61616");
		
		TomoBean tbean = new TomoBean();
		tbean.setName("Test Jython");
		tbean.setMessage("A test jython execution");
		tbean.setRunDirectory("C:/tmp/");

		final RemoteSubmission factory = new RemoteSubmission(uri);
		factory.setQueueName("scisoft.tomo.SUBMISSION_QUEUE");

		factory.submit(tbean, true);
	}
}
