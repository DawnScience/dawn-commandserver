package org.dawnsci.commandserver.core.producer;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.application.IConsumerExtension;
import org.dawnsci.commandserver.core.consumer.Constants;
import org.dawnsci.commandserver.core.consumer.ConsumerBean;
import org.dawnsci.commandserver.core.consumer.ConsumerStatus;
import org.dawnsci.commandserver.core.util.JSONUtils;

public abstract class AliveConsumer implements IConsumerExtension {

	private URI                  uri;

	protected Connection         aliveConnection;
	protected final String       consumerId;
	protected String             consumerVersion;

	private boolean active = true;

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
		final ConsumerBean cbean = new ConsumerBean();
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

		
		JSONUtils.sendTopic(cbean, Constants.ALIVE_TOPIC, uri);
		System.out.println("Running events on topic "+Constants.ALIVE_TOPIC+" to notify of '"+getName()+"' service being available.");
		
		cbean.setStatus(ConsumerStatus.RUNNING);
		
		final Thread aliveThread = new Thread(new Runnable() {
			public void run() {
				
				try {
					ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);		
					aliveConnection = connectionFactory.createConnection();
					aliveConnection.start();
	
					while(isActive()) {
						try {
							Thread.sleep(Constants.NOTIFICATION_FREQUENCY);
							JSONUtils.sendTopic(aliveConnection, cbean, Constants.ALIVE_TOPIC, uri);
							
						} catch (InterruptedException ne) {
							break;
						} catch (Exception neOther) {
							neOther.printStackTrace();
						}
					}
				} catch (Exception ne) {
					ne.printStackTrace();
				}
			}
		});
		aliveThread.setName("Alive Notification Topic");
		aliveThread.setDaemon(true);
		aliveThread.setPriority(Thread.MIN_PRIORITY);
		aliveThread.start();
	}

	
	public void stop() throws Exception {
		if (aliveConnection!=null) aliveConnection.close();
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
