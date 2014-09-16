package org.dawnsci.commandserver.core.producer;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.application.IConsumerExtension;
import org.dawnsci.commandserver.core.consumer.Constants;
import org.dawnsci.commandserver.core.consumer.ConsumerBean;
import org.dawnsci.commandserver.core.consumer.ConsumerStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AliveConsumer implements IConsumerExtension {

	private URI                  uri;

	protected Connection         aliveConnection;
	protected final String       consumerId;
	protected String             consumerVersion;

	private boolean active = true;

	private Connection terminateConnection;
	private ConsumerBean cbean;

	protected AliveConsumer() {
		this.consumerId      = System.currentTimeMillis()+"_"+UUID.randomUUID().toString();
		this.consumerVersion = "1.0";
	}
	
	/**
	 * 
	 * @return the name which the user will see for this consumer.
	 */
	public abstract String getName();

	protected void startNotifications() throws Exception {
		
		if (uri==null) throw new NullPointerException("Please set the URI before starting notifications!");
		this.cbean = new ConsumerBean();
		cbean.setStatus(ConsumerStatus.STARTING);
		cbean.setName(getName());
		cbean.setConsumerId(consumerId);
		cbean.setVersion(consumerVersion);
		cbean.setStartTime(System.currentTimeMillis());
		try {
			cbean.setHostName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			// Not fatal but would be nice...
			e.printStackTrace();
		}

		System.out.println("Running events on topic "+Constants.ALIVE_TOPIC+" to notify of '"+getName()+"' service being available.");
		
		
		final Thread aliveThread = new Thread(new Runnable() {
			public void run() {
				
				try {
					ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);		
					aliveConnection = connectionFactory.createConnection();
					aliveConnection.start();
	
					final Session         session  = aliveConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
					final Topic           topic    = session.createTopic(Constants.ALIVE_TOPIC);
					final MessageProducer producer = session.createProducer(topic);

					final ObjectMapper mapper = new ObjectMapper();

					// Here we are sending the message out to the topic
					while(isActive()) {
						try {
							Thread.sleep(Constants.NOTIFICATION_FREQUENCY);
							
							TextMessage temp = session.createTextMessage(mapper.writeValueAsString(cbean));
							producer.send(temp, DeliveryMode.NON_PERSISTENT, 1, 5000);
							
							if (ConsumerStatus.STARTING.equals(cbean.getStatus())) {
								cbean.setStatus(ConsumerStatus.RUNNING);
							}

						} catch (InterruptedException ne) {
							break;
						} catch (Exception neOther) {
							neOther.printStackTrace();
						}
					}
				} catch (Exception ne) {
					ne.printStackTrace();
					setActive(false);
				}
			}
		});
		aliveThread.setName("Alive Notification Topic");
		aliveThread.setDaemon(true);
		aliveThread.setPriority(Thread.MIN_PRIORITY);
		aliveThread.start();
	}

    protected void createTerminateListener() throws Exception {
		
    	ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
    	this.terminateConnection = connectionFactory.createConnection();
    	terminateConnection.start();

    	Session session = terminateConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

    	final Topic           topic    = session.createTopic(Constants.TERMINATE_CONSUMER_TOPIC);
    	final MessageConsumer consumer = session.createConsumer(topic);

    	final ObjectMapper mapper = new ObjectMapper();

    	MessageListener listener = new MessageListener() {
    		public void onMessage(Message message) {		            	
    			try {
    				if (message instanceof TextMessage) {
    					TextMessage t = (TextMessage) message;
    					final ConsumerBean bean = mapper.readValue(t.getText(), ConsumerBean.class);

    					if (bean.getStatus().isFinal()) { // Something else already happened
    						terminateConnection.close();
    						return;
    					}

    					if (consumerId.equals(bean.getConsumerId())) {
    						if (bean.getStatus() == ConsumerStatus.REQUEST_TERMINATE) {
    							System.out.println(getName()+" has been requested to terminate and will exit.");
    							cbean.setStatus(ConsumerStatus.REQUEST_TERMINATE);
    							Thread.currentThread().sleep(2500);
    							System.exit(0);
    						}
    					}

    				}
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}

    	};
    	consumer.setMessageListener(listener);

    }

	
	public void stop() throws Exception {
		if (aliveConnection!=null)     aliveConnection.close();
		if (terminateConnection!=null) terminateConnection.close();
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	protected final static boolean checkArguments(String[] args, String usage) {
		
        if (args == null || args.length!=4) {
        	System.out.println(usage);
        	return false;
        }
        
        if (!args[0].startsWith("tcp://")) {
        	System.out.println(usage);
        	return false;
        }
        
        if ("".equals(args[1])) {
        	System.out.println(usage);
        	return false;
        }
        
        if ("".equals(args[2])) {
        	System.out.println(usage);
        	return false;
        }
        
        if ("".equals(args[3])) {
        	System.out.println(usage);
        	return false;
        }
        
        return true;

	}
	protected URI getUri() {
		return uri;
	}

	protected void setUri(URI uri) {
		this.uri = uri;
	}

}
