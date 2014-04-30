package org.dawnsci.commandserver.core.util;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils {

	/**
	 * Generic way of sending a topic notification
	 * @param message
	 * @param topicName
	 * @param uri
	 * @throws Exception
	 */
	public static final void sendTopic(Object message, String topicName, String uri) throws Exception {

		
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
 	        
            // Here we are sending the message out to the topic
            producer.send(session.createTextMessage(mapper.writeValueAsString(message)));

		} finally {
			if (connection!=null) connection.close();
		}
	}

}
