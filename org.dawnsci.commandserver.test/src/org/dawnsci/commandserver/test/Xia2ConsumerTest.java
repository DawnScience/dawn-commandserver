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

	private volatile boolean ok = false;
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
		
		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(new URI("tcp://sci-serv5.diamond.ac.uk:61616"));
		Connection topicConnection = connectionFactory.createConnection();
        try {
        	topicConnection.start();

        	Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        	final Topic           topic    = session.createTopic(Constants.ALIVE_TOPIC);
        	final MessageConsumer consumer = session.createConsumer(topic);

            ok = false;
        	MessageListener listener = new MessageListener() {
        		public void onMessage(Message message) {		            	
        			try {
        				if (message instanceof TextMessage) {
        					TextMessage t = (TextMessage) message;
        					System.out.println(t.toString());
        					ok = true;
        				}
        			} catch (Exception e) {
        				throw e;
        			}
        		}
        	};
        	consumer.setMessageListener(listener);

            Thread.sleep(10000);
            
            if (!ok) throw new Exception("Xia2 Consumer Heartbeat not encountered!");
        	
        } finally {
            topicConnection.close();
        }
	}
}
