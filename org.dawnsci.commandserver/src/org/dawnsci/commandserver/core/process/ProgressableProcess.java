package org.dawnsci.commandserver.core.process;

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
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
import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Extend to provide a connection between a running process 
 * and its 
 * 
 * @author fcp94556
 *
 */
public abstract class ProgressableProcess implements Runnable {

	private boolean            isCancelled = false;
	protected final StatusBean bean;
	protected final String     uri;
	protected final String     statusTName;
	protected final String     statusQName;

	public ProgressableProcess(final String uri, final String statusTName, final String   statusQName, StatusBean bean) {
		this.uri           = uri;
		this.statusTName     = statusTName;
		this.statusQName   = statusQName;
		this.bean          = bean;
		bean.setStatus(Status.QUEUED);
		broadcast(bean);
	}

	/**
	 * Call to start the process and broadcast status
	 * updates. Subclasses may redefine what is done
	 * on the start method, by default a thread is started
	 * in daemon mode to run things.
	 */
	public void start() {
		
		final Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.setPriority(Thread.MAX_PRIORITY);
		thread.start();
	}

	/**
	 * Notify any clients of the beans status
	 * @param bean
	 */
	protected void broadcast(StatusBean bean) {
		try {
			updateQueue(bean); // For clients connecting in future or after a refresh - persistence.
			sendTopic(bean);   // For topic listeners wait for updates (more efficient than polling queue)
		} catch (Exception e) {
			e.printStackTrace();
		}
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
			Queue queue   = qSes.createQueue(statusQName);
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
		
		Connection connection = null;
		try {
			ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);		
			connection = connectionFactory.createConnection();
	        connection.start();
	
	        // JMS messages are sent and received using a Session. We will
	        // create here a non-transactional session object. If you want
	        // to use transactions you should set the first parameter to 'true'
	        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	
	        Topic topic = session.createTopic(statusTName);
	
	        MessageProducer producer = session.createProducer(topic);
	
            final ObjectMapper mapper = new ObjectMapper();
 	        
            // Here we are sending the message out to the topic
            producer.send(session.createTextMessage(mapper.writeValueAsString(bean)));

		} finally {
			if (connection!=null) connection.close();
		}
	}

	public boolean isCancelled() {
		return isCancelled;
	}


	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
}
