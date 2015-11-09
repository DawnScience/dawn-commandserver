package org.dawnsci.commandserver.test;

import java.net.URI;

import org.dawnsci.commandserver.core.consumer.HeartbeatChecker;
import org.dawnsci.commandserver.mx.consumer.MXSubmissionConsumer;
import org.junit.Test;

/**
 * This test checks a required consumer is available and 
 * fails if it is not. If the consumer is not available, it should
 * be restarted.
 * 
 * @author fcp94556
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
	public void testXia2BeingConsumed() throws Exception {
		
		HeartbeatChecker checker = new HeartbeatChecker(new URI("tcp://sci-serv5.diamond.ac.uk:61616"), MXSubmissionConsumer.NAME, 10000);
		checker.checkPulse();
		System.out.println("The patient "+MXSubmissionConsumer.NAME+" is alive and well Dr.");
	}
}
