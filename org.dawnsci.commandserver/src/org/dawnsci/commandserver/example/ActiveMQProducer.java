/*-
 * Copyright (c) 2013 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.example;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Running this playing around example:
 * 
 * 1. Install activemq 5.9 and start it. From cmd window in windows for example:
   C:\ActiveMQ\apache-activemq-5.9.0\bin>set JAVA_HOME=C:\Program Files\Java\jdk1.7.0_51
   C:\ActiveMQ\apache-activemq-5.9.0\bin>activemq
   
   2. Run this main method which sends 
   
   For command servers using activemq, current idea might be to have:
   1. One eclipse product for each consumer, mx multi-xstall, tomo, ncd saxs/waxs.
   2. They may be started and stopped independently using separate code to start the consumers by product.
   3. They use DRMAA code to run things on the cluster and share this code and any other analysis code.
   4. As producer runs it sends message events back to provide progress.
   
 * 
 * @author fcp94556
 *
 */
public class ActiveMQProducer {


	public static void main(String[] args) throws Exception {
		
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://DIAMRL5294:61616");
		Connection send = connectionFactory.createConnection();
		
		Session session = send.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue destination = session.createQueue("testQ");
		
		final MessageProducer producer = session.createProducer(destination);
		producer.setDeliveryMode(DeliveryMode.PERSISTENT);
		
		Message message = session.createTextMessage("Hello World");
		producer.send(message);
		
		message = session.createTextMessage("...and another message");
		producer.send(message);
		
		message = session.createObjectMessage(new TestObjectBean("this could be", "anything"));
	    producer.send(message);
		
		session.close();
		send.close();
	}


}
