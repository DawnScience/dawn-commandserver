package org.dawnsci.commandserver.core.producer;

import java.io.PrintStream;
import java.net.URI;
import java.util.Enumeration;

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
import javax.jms.Topic;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.consumer.RemoteSubmission;

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
	
	private PrintStream out  = System.out;

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

		if (connection==null) createConnection();
        if (add) {
			addQueue(bean);
		} else {
		    updateQueue(bean);  // For clients connecting in future or after a refresh - persistence.
		}
        
		final ObjectMapper mapper = new ObjectMapper();

		// Here we are sending the message out to the topic
		TextMessage temp = session.createTextMessage(mapper.writeValueAsString(bean));
		topicProducer.send(temp, DeliveryMode.NON_PERSISTENT, 1, 5000);

		if (out!=null) {
			out.println(bean.toString());
			out.flush();
		}
		
		if (bean.getStatus().isFinal()) { // No more updates!
			dispose();
		}

 	}
	
	private QueueConnection connection;
	private QueueSession    qSes;
	private Queue           queue;
	private MessageProducer topicProducer;
	private Session         session;

	public void dispose() throws JMSException {
		try {
        	if (out!=null && out!=System.out) {
        		out.close();
        		out = null;
        	}

			qSes.close();
			session.close();
			connection.close();

		} finally {
			connection = null;
			qSes = null;
			queue = null;
			topicProducer = null;
			session = null;
		}
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
	
	private void createConnection() throws Exception {
		
 	    QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
 	    this.connection  = connectionFactory.createQueueConnection(); 
		this.qSes        = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		this.queue       = qSes.createQueue(queueName);

		this.session       = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		final Topic topic  = session.createTopic(topicName);
		this.topicProducer = session.createProducer(topic);
		
		connection.start();

	}

	/**
	 * 
	 * @param bean
	 * @throws Exception 
	 */
	private void updateQueue(StatusBean bean) throws Exception {
				
		QueueConnection qCon = null;
		
		try {
			
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
	        
	        qb.close();
	        
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

	public URI getUri() {
		return uri;
	}

	public String getQueueName() {
		return queueName;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setLoggingStream(PrintStream out) {
		this.out = out;
	}

}
