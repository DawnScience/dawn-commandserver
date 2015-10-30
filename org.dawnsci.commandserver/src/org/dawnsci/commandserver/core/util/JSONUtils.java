/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.core.util;

import java.net.URI;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.eclipse.scanning.api.event.IEventConnectorService;

public class JSONUtils {

	public static final void sendTopic(Object message, String topicName, URI uri) throws Exception {

		
		Connection connection = null;
		try {
			ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);		
			connection = connectionFactory.createConnection();
	        connection.start();
 
	        sendTopic(connection, message, topicName, uri);

		} finally {
			if (connection!=null) connection.close();
		}
	} 
	/**
	 * Generic way of sending a topic notification
	 * @param connection - does not get closed afterwards nust be started before.
	 * @param message
	 * @param topicName
	 * @param uri
	 * @throws Exception
	 */
	private static final void sendTopic(Connection connection, Object message, String topicName, URI uri) throws Exception {

		// JMS messages are sent and received using a Session. We will
		// create here a non-transactional session object. If you want
		// to use transactions you should set the first parameter to 'true'
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		try {
			Topic topic = session.createTopic(topicName);
	
			MessageProducer producer = session.createProducer(topic);
	
			final IEventConnectorService service = ActiveMQServiceHolder.getEventConnectorService();
	
			// Here we are sending the message out to the topic
			TextMessage temp = session.createTextMessage(service.marshal(message));
			producer.send(temp, DeliveryMode.NON_PERSISTENT, 1, 5000);
			
		} finally {
			session.close();
		}
	}

}
