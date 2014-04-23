package org.dawnsci.commandserver.core;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Extend to provide a connection between a running process 
 * and its 
 * 
 * @author fcp94556
 *
 */
public abstract class ProgressableProcess implements Runnable {

	private boolean isCancelled = false;
	protected final StatusBean bean;
	protected final String uri;
	protected final String topicName;
	private TextMessage queuedMessage;

	public ProgressableProcess(final String uri, final String topicName, TextMessage queuedMessage, StatusBean bean) {
		this.uri           = uri;
		this.topicName     = topicName;
		this.queuedMessage = queuedMessage;
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
		
		Connection connection = null;
		try {
			ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);		
			connection = connectionFactory.createConnection();
	        connection.start();
	
	        // JMS messages are sent and received using a Session. We will
	        // create here a non-transactional session object. If you want
	        // to use transactions you should set the first parameter to 'true'
	        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	
	        Topic topic = session.createTopic(topicName);
	
	        MessageProducer producer = session.createProducer(topic);
	
            final ObjectMapper mapper = new ObjectMapper();
            queuedMessage.setText(mapper.writeValueAsString(bean));
	        
	        // Here we are sending the message!
	        producer.send(queuedMessage);

		} catch (Exception ne) {
			ne.printStackTrace();
			
		} finally {
			try {
				if (connection!=null) connection.close();
			} catch (JMSException e) {
				e.printStackTrace();
			}	
		}
 	}


	public boolean isCancelled() {
		return isCancelled;
	}


	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
}
