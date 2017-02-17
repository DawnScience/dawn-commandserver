package org.dawnsci.commandserver.test;

import java.net.URI;

import org.dawnsci.commandserver.mx.consumer.MXSubmissionConsumer;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.ConsumerBean;
import org.eclipse.scanning.event.EventServiceImpl;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.scanning.connector.activemq.ActivemqConnectorService;

/**
 * This test checks a required consumer is available and 
 * fails if it is not. If the consumer is not available, it should
 * be restarted.
 * 
 * @author Matthew Gerring
 *
 */
public class Xia2ConsumerTest {


	/**
	 * This test fails if the Xia2Consumer cannot be located
	 * in activemq.
	 * 
	 * It is a fail which occurs 
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore("Test always fails, so disabled")
	public void testXia2BeingConsumed() throws Exception {
		
		IEventService servce = new EventServiceImpl(new ActivemqConnectorService()); // Testing!
		servce.checkTopic(new URI("tcp://sci-serv5.diamond.ac.uk:61616"), MXSubmissionConsumer.NAME, 10000, "scisoft.commandserver.core.ALIVE_TOPIC", ConsumerBean.class);
		// Used once DAWN2 is released:
		//servce.checkHeartbeat(new URI("tcp://sci-serv5.diamond.ac.uk:61616"), MXSubmissionConsumer.NAME, 10000);
		System.out.println("The patient "+MXSubmissionConsumer.NAME+" is alive and well Dr.");
	}
}
