package org.dawnsci.commandserver.test;

import java.net.URI;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.consumer.Constants;
import org.dawnsci.commandserver.core.consumer.ConsumerBean;
import org.dawnsci.commandserver.core.consumer.HeartbeatChecker;
import org.dawnsci.commandserver.mx.consumer.MXSubmissionConsumer;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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
