package org.dawnsci.commandserver.test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.eclipse.scanning.api.event.IEventConnectorService;
import org.junit.Test;

import uk.ac.diamond.daq.activemq.connector.ActivemqConnectorService;

/**
 * Sorry for all the repeated code in this test!
 * Not a serious test, just trying to play around with queues.
 * 
 * @author fcp94556
 *
 */
public class ReorderQueueTest {

	
	@Test
	public void reoder() throws Exception {
		
		String testQueueName = "fred";
		URI uri = new URI("tcp://sci-serv5.diamond.ac.uk:61616");

		try {
			clearAnyOldTestQueue(testQueueName,uri);
		    createTestQueue(testQueueName, uri); // 10 items
		    
		    System.out.println("The current queue items are:");
		    printQueue(testQueueName, uri);
		    
		    // Move item to head by setting to a higher priority
		    reorder(testQueueName, uri, 4, 0);
		    System.out.println("The reordered items are:");
		    printQueue(testQueueName, uri);
		    
		} finally {
			clearAnyOldTestQueue(testQueueName,uri);
		}
	}


	private void reorder(String qName, URI uri, int fromIndex, int toIndex) throws Exception {
		
		
		// TODO Could send topic to pause the consumer(s) of this queue.
		
		QueueConnection qCon = null;
		try {
			IEventConnectorService service           = new ActivemqConnectorService();
			QueueConnectionFactory connectionFactory = (QueueConnectionFactory)service.createConnectionFactory(uri);
			qCon  = connectionFactory.createQueueConnection(); // This times out when the server is not there.
			QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue   = qSes.createQueue(qName);
			qCon.start();
			
		    QueueBrowser qb = qSes.createBrowser(queue);
		    
		    List<Message> messages = new ArrayList<Message>();
	    
		    @SuppressWarnings("rawtypes")
			Enumeration  e  = qb.getEnumeration();					
			while(e.hasMoreElements()) {
				Message msg = (Message)e.nextElement();
				if (msg instanceof TextMessage) {
					
					
					MessageConsumer consumer = qSes.createConsumer(queue, "JMSMessageID = '"+msg.getJMSMessageID()+"'");
					Message rem = consumer.receive(1000);	
					if (rem==null) {
						// Things can get consumed from the queue while we are reordering it.
						// If this was real code with an active consumer, we would have to deal
						// with the fact that the indices might not work anymore, but for this
						// example it is not a problem.
                        continue;
					}

					consumer.close();

					messages.add(rem);
				}
			}
			
			Message m = messages.remove(fromIndex);
			messages.add(toIndex, m);
			
			MessageProducer producer = qSes.createProducer(queue);
			for (Message message : messages) producer.send(message);
						
		} finally {
			if (qCon!=null) {
				qCon.close();
			}
		}
	}


	private void printQueue(String qName, URI uri) throws JMSException {
		
		QueueConnection qCon = null;
		try {
			IEventConnectorService service           = new ActivemqConnectorService();
			QueueConnectionFactory connectionFactory = (QueueConnectionFactory)service.createConnectionFactory(uri);
			qCon  = connectionFactory.createQueueConnection(); // This times out when the server is not there.
			QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue   = qSes.createQueue(qName);
			qCon.start();
			
		    QueueBrowser qb = qSes.createBrowser(queue);
	    
		    @SuppressWarnings("rawtypes")
			Enumeration  e  = qb.getEnumeration();

	        while(e.hasMoreElements()) {
		    	Message m = (Message)e.nextElement();
		    	if (m==null) continue;
	        	if (m instanceof TextMessage) {
	            	TextMessage t = (TextMessage)m;
					System.out.println(t.getText());
	        	}
		    }
	        
		} finally {
			qCon.close();
		}
	}


	private void createTestQueue(String queueName, URI uri) throws Exception {
		
		Connection      send     = null;
		try {
			IEventConnectorService service           = new ActivemqConnectorService();
			QueueConnectionFactory connectionFactory = (QueueConnectionFactory)service.createConnectionFactory(uri);
			send              = connectionFactory.createConnection();

			Session session = send.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue(queueName);

			MessageProducer producer = session.createProducer(queue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			
			for (int i = 0; i < 10; i++) {
				TextMessage message = session.createTextMessage("Message_"+String.valueOf(i));
				producer.send(message);
			}
		
		} finally {
			if (send!=null)     send.close();
		}
		
	}

	private void clearAnyOldTestQueue(String qName, URI uri) throws Exception {
		
		QueueConnection qCon = null;
		try {
			IEventConnectorService service           = new ActivemqConnectorService();
			QueueConnectionFactory connectionFactory = (QueueConnectionFactory)service.createConnectionFactory(uri);
			qCon  = connectionFactory.createQueueConnection(); // This times out when the server is not there.
			QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue   = qSes.createQueue(qName);
			qCon.start();
			
		    QueueBrowser qb = qSes.createBrowser(queue);
	    
		    @SuppressWarnings("rawtypes")
			Enumeration  e  = qb.getEnumeration();					
			while(e.hasMoreElements()) {
				Message msg = (Message)e.nextElement();
	        	MessageConsumer consumer = qSes.createConsumer(queue, "JMSMessageID = '"+msg.getJMSMessageID()+"'");
	        	Message rem = consumer.receive(1000);	
	        	if (rem!=null) System.out.println("Removed "+rem);
			    consumer.close();
			}
						
		} finally {
			if (qCon!=null) {
				qCon.close();
			}
		}
	}
}
