package org.dawnsci.commandserver.core.consumer;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.eclipse.core.runtime.IProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reads a Queue of json beans from the activemq queue and deserializes the 
 * json beans to a specific class.
 * 
 * @author fcp94556
 *
 * @param <T>
 */
public class QueueReader<T> {
	
	private static final Logger logger = LoggerFactory.getLogger(QueueReader.class);
	
	private Comparator comparator;

	public QueueReader() {
		this(null);
	}
	
	public QueueReader(Comparator comparator) {
		this.comparator = comparator;
	}

	
	/**
	 * Read the status beans from any queue.
	 * Returns a list of optionally date-ordered beans in the queue.
	 * 
	 * @param uri
	 * @param queueName
	 * @param clazz
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public Collection<T> getBeans(final URI uri, final String queueName, final Class<?> clazz, final IProgressMonitor monitor) throws Exception {
		
		QueueConnection qCon = null;
		try {
			QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			monitor.worked(1);
			qCon  = connectionFactory.createQueueConnection(); // This times out when the server is not there.
			QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue   = qSes.createQueue(queueName);
			qCon.start();
			
		    QueueBrowser qb = qSes.createBrowser(queue);
			monitor.worked(1);
	    
		    @SuppressWarnings("rawtypes")
			Enumeration  e  = qb.getEnumeration();
		    
			ObjectMapper mapper = new ObjectMapper();
			
			final Collection<T> list;
			if (comparator!=null) {
				list = new TreeSet<T>(comparator);
			} else {
				list = new ArrayList<T>(17);
			}
	
	        while(e.hasMoreElements()) {
		    	Message m = (Message)e.nextElement();
		    	if (m==null) continue;
	        	if (m instanceof TextMessage) {
	            	TextMessage t = (TextMessage)m;
					final T bean = (T)mapper.readValue(t.getText(), clazz);
	              	list.add(bean);
	        	}
		    }
	        return list;
	        
		} finally {
			qCon.close();
		}
	}

	/**
	 * 
	 * @param uri
	 * @param topicName
	 * @param clazz
	 * @param monitorTime
	 * @return
	 */
	public Map<String, T> getHeartbeats(final URI uri, final String topicName, final Class<?> clazz, final long monitorTime) throws Exception {
		
		final Map<String, T> ret = new HashMap<String, T>(3);
		Connection topicConnection = null;
		try {
			ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			topicConnection = connectionFactory.createConnection();
			topicConnection.start();

			Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			final Topic           topic    = session.createTopic(Constants.ALIVE_TOPIC);
			final MessageConsumer consumer = session.createConsumer(topic);

			final ObjectMapper mapper = new ObjectMapper();

			MessageListener listener = new MessageListener() {
				public void onMessage(Message message) {		            	
					try {
						if (message instanceof TextMessage) {
							TextMessage t = (TextMessage) message;
							final T bean = (T)mapper.readValue(t.getText(), clazz);
							Method nameMethod = bean.getClass().getMethod("getName");
							ret.put((String)nameMethod.invoke(bean), bean);
						}
					} catch (Exception e) {
						logger.error("Updating changed bean from topic", e);
					}
				}
			};
			consumer.setMessageListener(listener);
			Thread.sleep(monitorTime);
			
			return ret;

		} catch (Exception ne) {
			logger.error("Cannot listen to topic changes because command server is not there", ne);
			return null;
		} finally {
			topicConnection.close();
		}

	}
}
