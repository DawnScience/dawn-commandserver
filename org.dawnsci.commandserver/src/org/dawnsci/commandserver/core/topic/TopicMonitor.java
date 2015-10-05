package org.dawnsci.commandserver.core.topic;

import java.net.URI;
import java.util.ArrayList;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
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
 * Class to monitor a topic a notify listener(s) with the deserialized bean.
 * 
 * @author Matthew Gerring
 *
 */
public class TopicMonitor<T> {
	
	private static Logger logger = LoggerFactory.getLogger(TopicMonitor.class);
	
	private Connection                  topicConnection;
	private URI                         uri;
	private Class<T>                    beanClass;
	private String                      topicId;

	private ArrayList<BeanChangeListener<T>> listeners;
	
	public TopicMonitor(URI uri, Class<T> beanClass, String topic) {
		this.uri = uri;
		this.beanClass = beanClass;
		this.topicId = topic;
	}
	
	public void addBeanChangeListener(BeanChangeListener<T> listener) {
		if (listeners == null) listeners = new ArrayList<BeanChangeListener<T>>(3);
		listeners.add(listener);
	}
	public void removeBeanChangeListener(BeanChangeListener<T> listener) {
		if (listeners == null) return;
		listeners.remove(listener);
	}
	public void fireBeanChangeListeners(final T bean) {
		
		final BeanChangeEvent<T> evt = new BeanChangeEvent<T>(bean);
		final BeanChangeListener<T>[] snapshot = listeners.toArray(new BeanChangeListener[listeners.size()]);
		for (BeanChangeListener<T> l : snapshot) {
			l.beanChangePerformed(evt);
		}
	}

	public void connect() throws Exception {
		
		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
	    topicConnection = connectionFactory.createConnection();
	    topicConnection.start();
	
	    Session session = topicConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	
	    final Topic           topic    = session.createTopic(topicId);
	    final MessageConsumer consumer = session.createConsumer(topic);
	
	    final ObjectMapper mapper = new ObjectMapper();
	    
	    MessageListener listener = new MessageListener() {
	        public void onMessage(Message message) {		            	
	            try {
	                if (message instanceof TextMessage) {
	                    TextMessage t = (TextMessage) message;
	    				final T bean = (T)mapper.readValue(t.getText(), beanClass);
	    				fireBeanChangeListeners(bean);
	                }
	            } catch (Exception e) {
	                logger.error("Updating changed bean from topic", e);
	            }
	        }
	    };
	    consumer.setMessageListener(listener);
	}
	
	public void close() throws JMSException {
		topicConnection.close();
	}

}
