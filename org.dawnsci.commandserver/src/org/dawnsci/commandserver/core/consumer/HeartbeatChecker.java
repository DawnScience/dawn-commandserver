package org.dawnsci.commandserver.core.consumer;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Checks for the heartbeat of a named consumer.
 * 
 * @author fcp94556
 *
 */
public class HeartbeatChecker {
	
	private static final Logger logger = LoggerFactory.getLogger(HeartbeatChecker.class);

	private URI    uri;
	private String consumerName;
	private long   listenTime;
	private volatile boolean ok = false;
	
	public HeartbeatChecker(URI uri, String consumerName, long listenTime) {
		this.uri          = uri;
		this.consumerName = consumerName;
		this.listenTime   = listenTime;
	}
	
	public void checkPulse() throws Exception {
		
		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
		Connection topicConnection = connectionFactory.createConnection();
        try {
        	topicConnection.start();

        	Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        	final Topic           topic    = session.createTopic(Constants.ALIVE_TOPIC);
        	final MessageConsumer consumer = session.createConsumer(topic);

	        ok = false;
	        
	        final ObjectMapper mapper = new ObjectMapper();
	        final Thread runnerThread = Thread.currentThread();
	        
        	MessageListener listener = new MessageListener() {
        		public void onMessage(Message message) {		            	
        			try {
        				if (message instanceof TextMessage) {
        					TextMessage t = (TextMessage) message;
        					ConsumerBean  b = mapper.readValue(t.getText(), ConsumerBean.class);
        					if (!consumerName.equals(b.getName())) {
        						return;
        					}
        					logger.trace(b.getName()+ " is alive and well.");
        					ok = true;
        				}
        			} catch (Exception e) {
        				ok = false;
        				e.printStackTrace();
        				runnerThread.interrupt(); // Stop it sleeping...
        			}
        		}
        	};
        	consumer.setMessageListener(listener);

            Thread.sleep(listenTime);
            
            if (!ok) throw new Exception(consumerName+" Consumer heartbeat absent.\nIt is either stopped or unresponsive.\nPlease contact your support representative.");
        	

        } finally {
            topicConnection.close();
        }
	}

}
