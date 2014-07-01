package org.dawnsci.commandserver.core.producer;

import java.net.URI;
import java.util.Enumeration;

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

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.consumer.RemoteSubmission;
import org.dawnsci.commandserver.core.util.JSONUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class which can broadcast a status bean to a queue and a topic
 * when the broadcast method is called.
 * 
 * @author fcp94556
 *
 */
public class Broadcaster {

	
	private final URI uri;
	private final String queueName;
	private final String topicName;

	public Broadcaster(URI uri, String queueName, String topicName) {
		this.uri       = uri;
		this.queueName = queueName;
		this.topicName = topicName;
	}

	/**
	 * Notify any clients of the beans status
	 * @param bean
	 * @param add true to add a new message, false to find and update and old one.
	 */
	public void broadcast(StatusBean bean, boolean add) throws Exception {
		if (add) {
			addQueue(bean);
		} else {
		    updateQueue(bean);  // For clients connecting in future or after a refresh - persistence.
		}
		sendTopic(bean);  // For topic listeners wait for updates (more efficient than polling queue)
 	}

	/**
	 * 
	 * @param bean
	 * @throws Exception 
	 */
	private void addQueue(StatusBean bean) throws Exception {
		
    	RemoteSubmission factory = new RemoteSubmission(uri);
     	factory.setQueueName(queueName); // Move the message over to a status queue.
    	
    	factory.submit(bean, false);
		
	}

	/**
	 * 
	 * @param bean
	 * @throws Exception 
	 */
	private void updateQueue(StatusBean bean) throws Exception {
		
		QueueConnection qCon = null;
		
		try {
	 	    QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			qCon  = connectionFactory.createQueueConnection(); 
			QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue   = qSes.createQueue(queueName);
			qCon.start();
			
		    QueueBrowser qb = qSes.createBrowser(queue);
		    
		    @SuppressWarnings("rawtypes")
			Enumeration  e  = qb.getEnumeration();
		    
			ObjectMapper mapper = new ObjectMapper();
			String jMSMessageID = null;
	        while(e.hasMoreElements()) {
		    	Message m = (Message)e.nextElement();
		    	if (m==null) continue;
	        	if (m instanceof TextMessage) {
	            	TextMessage t = (TextMessage)m;
	              	
	            	@SuppressWarnings("unchecked")
					final StatusBean qbean = mapper.readValue(t.getText(), bean.getClass());
	            	if (qbean==null)               continue;
	            	if (qbean.getUniqueId()==null) continue; // Definitely not our bean
	            	if (qbean.getUniqueId().equals(bean.getUniqueId())) {
	            		jMSMessageID = t.getJMSMessageID();
	            		break;
	            	}
	        	}
		    }
	        
	        if (jMSMessageID!=null) {
	        	MessageConsumer consumer = qSes.createConsumer(queue, "JMSMessageID = '"+jMSMessageID+"'");
	        	Message m = consumer.receive(1000);
	        	if (m!=null && m instanceof TextMessage) {
	        		MessageProducer producer = qSes.createProducer(queue);
	        		producer.send(qSes.createTextMessage(mapper.writeValueAsString(bean)));
	        	}
	        }
		} finally {
			if (qCon!=null) qCon.close();
		}
		
	}

	private void sendTopic(StatusBean bean) throws Exception {
		JSONUtils.sendTopic(bean, topicName, uri);
	}

	public URI getUri() {
		return uri;
	}

	public String getQueueName() {
		return queueName;
	}

	public String getTopicName() {
		return topicName;
	}

}
