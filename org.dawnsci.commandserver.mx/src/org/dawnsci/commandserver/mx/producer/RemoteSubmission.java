package org.dawnsci.commandserver.mx.producer;

import java.util.UUID;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.Session;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.mx.beans.DataCollectionsBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class RemoteSubmission {
	
	private String uri;
	private String messageId;
	private String queueName;
	private int    priority;
	private long   lifeTime;
	private long   timestamp;
	
	public RemoteSubmission(String uri) {
	    this.uri       = uri;
	    this.messageId = System.currentTimeMillis()+"_"+UUID.randomUUID();
	}

	/**
	 * Submits the bean onto the server. From there events about this
	 * bean are tacked by monitoring the status queue.
	 * 
	 * @param uri
	 * @param bean
	 */
	public synchronized void submit(DataCollectionsBean bean) throws Exception {

		
		if (getQueueName()==null || "".equals(getQueueName())) throw new Exception("Please specify a queue name!");
		
		if (bean.getName()==null) bean.createName();
		
		Connection      send     = null;
		Session         session  = null;
		MessageProducer producer = null;
		
		try {
			QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			send              = connectionFactory.createConnection();

			session = send.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue = session.createQueue(queueName);

			producer = session.createProducer(queue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			ObjectMapper mapper = new ObjectMapper();			
			if (bean.getUserName()==null)   bean.setUserName(System.getProperty("user.name"));
			String   jsonString = mapper.writeValueAsString(bean);
			
			Message message = session.createTextMessage(jsonString);
			
			if (getTimestamp()<1) setTimestamp(System.currentTimeMillis());
			if (getPriority()<1)  setPriority(1);
			if (getLifeTime()<1)  setLifeTime(7*24*60*60*1000); // 7 days in ms

			message.setJMSMessageID(messageId);
			message.setJMSExpiration(getLifeTime());
			message.setJMSTimestamp(getTimestamp());
			message.setJMSPriority(getPriority());
			
			producer.send(message);
			
		} finally {
			if (send!=null)     send.close();
			if (session!=null)  session.close();
			if (producer!=null) producer.close();
		}

	}

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public long getLifeTime() {
		return lifeTime;
	}

	public void setLifeTime(long lifeTime) {
		this.lifeTime = lifeTime;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (lifeTime ^ (lifeTime >>> 32));
		result = prime * result
				+ ((messageId == null) ? 0 : messageId.hashCode());
		result = prime * result + priority;
		result = prime * result
				+ ((queueName == null) ? 0 : queueName.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result + ((uri == null) ? 0 : uri.hashCode());
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
		RemoteSubmission other = (RemoteSubmission) obj;
		if (lifeTime != other.lifeTime)
			return false;
		if (messageId == null) {
			if (other.messageId != null)
				return false;
		} else if (!messageId.equals(other.messageId))
			return false;
		if (priority != other.priority)
			return false;
		if (queueName == null) {
			if (other.queueName != null)
				return false;
		} else if (!queueName.equals(other.queueName))
			return false;
		if (timestamp != other.timestamp)
			return false;
		if (uri == null) {
			if (other.uri != null)
				return false;
		} else if (!uri.equals(other.uri))
			return false;
		return true;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
}
