package org.dawnsci.commandserver.bubbles;

import java.net.URI;

public class ExampleSubmit {

	public static void main(String[] args) throws Exception {
		
		submitUsingJava();
	}

	private static void submitUsingJava() throws Exception {
		
		// URI could be anywhere I am using the scisoft activemq server
		// which should be fine for bubbles too.
		URI uri = new URI("tcp://sci-serv5.diamond.ac.uk:61616");
		
        // Just a bean that gets JSONed, the actual submission only requires
		// a JSON string, this is Java suger rather than required.
		BubblesBean bbean = new BubblesBean();
		bbean.setName("Test Execution (no bubbles executable)");
		bbean.setUserName(System.getProperty("user.name")); // Really fedid
		bbean.setMessage("Just a test!");
		bbean.setRunDirectory("C:/tmp/processing/");
		bbean.setIntensityPath("C:/tmp/processing/intensity.txt");
		bbean.setPofrPath("C:/tmp/processing/pofr.txt");
		
		// Object in Java that submits to JMS queue, same thing can be done
		// in just about any language.
//		final RemoteSubmission factory = new RemoteSubmission(uri);
//		factory.setQueueName("scisoft.bubbles.SUBMISSION_QUEUE");
//		factory.submit(bbean, true);
	}
}
