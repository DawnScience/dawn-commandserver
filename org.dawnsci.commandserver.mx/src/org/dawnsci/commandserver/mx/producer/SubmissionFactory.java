package org.dawnsci.commandserver.mx.producer;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.mx.beans.SubmissionBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SubmissionFactory {

	/**
	 * Submits the bean onto the server. From there
	 * @param uri
	 * @param bean
	 */
	public static void submit(String uri, SubmissionBean bean) throws Exception {
		
		if (bean.getQueueName()==null || "".equals(bean.getQueueName())) throw new Exception("Please specify a queue name!");
		
		Connection      send     = null;
		Session         session  = null;
		MessageProducer producer = null;
		
		try {
			QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			send              = connectionFactory.createConnection();

			session = send.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue(bean.getQueueName());

			producer = session.createProducer(queue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			ObjectMapper mapper = new ObjectMapper();
			String   jsonString = mapper.writeValueAsString(bean);
			
			Message message = session.createTextMessage(jsonString);
			producer.send(message);
			
		} finally {
			if (send!=null)     send.close();
			if (session!=null)  session.close();
			if (producer!=null) producer.close();
		}

	}
	
}
