package org.dawnsci.commandserver;

import java.io.Serializable;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Running this playing around example:
 * 
 * 1. Install activemq 5.9 and start it. From cmd window in windows for example:
   C:\ActiveMQ\apache-activemq-5.9.0\bin>set JAVA_HOME=C:\Program Files\Java\jdk1.7.0_51
   C:\ActiveMQ\apache-activemq-5.9.0\bin>activemq
   
   2. Run this main method which sends 
 * 
 * @author fcp94556
 *
 */
public class ActiveMQPlay {


	public static void main(String[] args) throws Exception {
		
		createConsumerThread();
		Thread.sleep(500);

		sendMessage();		
		Thread.sleep(500);
		
		System.exit(-1);
	}

	private static void sendMessage() throws Exception {
		
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

	private static void createConsumerThread() {
		
		final Thread consumer = new Thread(new Runnable() {
			public void run() {
				try {
					ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://DIAMRL5294:61616");
					Connection    connection = connectionFactory.createConnection();
					Session   session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
					Queue queue = session.createQueue("testQ");
	
					final MessageConsumer consumer = session.createConsumer(queue);
					connection.start();
					
		            while (true) {
		                Message m = consumer.receive(1);
                        if (m!=null) {
                        	if (m instanceof TextMessage) {
	                        	TextMessage t = (TextMessage)m;
	                        	System.out.println(t.getText());
                        	} else if (m instanceof ObjectMessage){
                        		ObjectMessage o = (ObjectMessage)m;
                        		System.out.println(o.getObject());
                        	}
                        }
					}
					
				} catch (Exception ne) {
					ne.printStackTrace();
				}

			}
		}, "Consumer thread");
		consumer.setDaemon(true);
		consumer.start();
		
	}
	
	public static class TestObjectBean implements Serializable {
        @Override
		public String toString() {
			return "TestObjectBean [m1=" + m1 + ", m2=" + m2 + "]";
		}
		/**
		 * 
		 */
		private static final long serialVersionUID = -4989930922999152348L;
		private String m1;
        private String m2;
		public TestObjectBean(String m1, String m2) {
			super();
			this.m1 = m1;
			this.m2 = m2;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((m1 == null) ? 0 : m1.hashCode());
			result = prime * result + ((m2 == null) ? 0 : m2.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestObjectBean other = (TestObjectBean) obj;
			if (m1 == null) {
				if (other.m1 != null)
					return false;
			} else if (!m1.equals(other.m1))
				return false;
			if (m2 == null) {
				if (other.m2 != null)
					return false;
			} else if (!m2.equals(other.m2))
				return false;
			return true;
		}
		public String getM1() {
			return m1;
		}
		public void setM1(String m1) {
			this.m1 = m1;
		}
		public String getM2() {
			return m2;
		}
		public void setM2(String m2) {
			this.m2 = m2;
		}
	}

}
