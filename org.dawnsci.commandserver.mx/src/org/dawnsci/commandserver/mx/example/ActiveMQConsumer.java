/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.mx.example;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.mx.beans.SweepBean;

import com.fasterxml.jackson.databind.ObjectMapper;

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
public class ActiveMQConsumer {


	public static void main(String[] args) throws Exception {
		
		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory("tcp://sci-serv5.diamond.ac.uk:61616");
		Connection    connection = connectionFactory.createConnection();
		Session   session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = session.createQueue("testQ");

		final MessageConsumer consumer = session.createConsumer(queue);
		connection.start();
		
        while (true) { // You have to kill it to stop it!
            Message m = consumer.receive(1000);
            if (m!=null) {
            	if (m instanceof TextMessage) {
                	TextMessage t = (TextMessage)m;
                	System.out.println(t.getText());
                	try {
                		ObjectMapper mapper = new ObjectMapper();
                		final SweepBean colBack = mapper.readValue(t.getText(), SweepBean.class);
                        System.out.println("Data collection found: "+colBack.getDataCollectionId());
                        
                	} catch (Exception ne) {
                		System.out.println(m+" is not a data collection.");
                	}
            	} else if (m instanceof ObjectMessage){
            		ObjectMessage o = (ObjectMessage)m;
            		System.out.println(o.getObject());
            	}
            }
		}
	}


}
