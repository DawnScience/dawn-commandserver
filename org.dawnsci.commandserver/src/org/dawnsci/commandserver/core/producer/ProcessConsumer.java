package org.dawnsci.commandserver.core.producer;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.consumer.RemoteSubmission;
import org.dawnsci.commandserver.core.process.ProgressableProcess;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * Please extend this consumer to create it and call the start method.
 * 
 * You must have the no argument constructor because the org.dawnsci.commandserver.core.application.Consumer
 * application requires this to start and stop the consumer.
 * 
 * @author fcp94556
 *
 */
public abstract class ProcessConsumer extends AliveConsumer {
	

	private String submitQName, statusTName, statusQName;
	protected Map<String, String> config;

	/**
	 * Method which configures the submission consumer for the queues and topics required.
	 * 
     * uri       activemq URI, e.g. tcp://sci-serv5.diamond.ac.uk:61616 
     * submit    queue to submit e.g. scisoft.xia2.SUBMISSION_QUEUE 
     * topic     topic to notify e.g. scisoft.xia2.STATUS_TOPIC 
     * status    queue for status e.g. scisoft.xia2.STATUS_QUEUE 
	 * 
	 * @param configuration
	 * @throws Exception
	 */
	public void init(Map<String, String> configuration) throws Exception {
		
		config = Collections.unmodifiableMap(configuration);
		setUri(new URI(config.get("uri")));
		this.submitQName = config.get("submit");
		this.statusTName = config.get("topic");
		this.statusQName = config.get("status");
	}

	/**
	 * Starts the consumer and does not return.
	 * @throws Exception
	 */
	public void start() throws Exception {
		
		startNotifications();

		processStatusQueue(getUri(), statusQName);
		
		// This is the blocker
		monitorSubmissionQueue(getUri(), submitQName, statusTName, statusQName);
	}
	
	/**
	 * You may override this method to stop the consumer cleanly. Please
	 * call super.stop() if you do.
	 * @throws JMSException 
	 */
	public void stop() throws Exception {
		
		setActive(false);
		super.stop();
	}

	/**
	 * Implement to return the actual bean class in the queue
	 * @return
	 */
	protected abstract Class<? extends StatusBean> getBeanClass();
	
	/**
	 * Implement to create the required command server process.
	 * 
	 * @param uri
	 * @param statusTName
	 * @param statusQName
	 * @param bean
	 * @return the process or null if the message should be consumed and nothing done.
	 */
	protected abstract ProgressableProcess createProcess(URI uri, String statusTName, String statusQName, StatusBean bean) throws Exception;

	protected static final long TWO_DAYS = 48*60*60*1000; // ms
	protected static final long A_WEEK = 7*24*60*60*1000; // ms
	/**
	 * Defines the time in ms that a job may be in the running state
	 * before the consumer might consider it for deletion. If a consumer
	 * is restarted it will normally delete old running jobs older than 
	 * this age.
	 * 
	 * @return
	 */
	protected abstract long getMaximumRunningAge();
	

	/**
	 * Defines the time in ms that a job may be in the complete (or other final) state
	 * before the consumer might consider it for deletion. If a consumer
	 * is restarted it will normally delete old complete jobs older than 
	 * this age.
	 * 
	 * @return
	 */
	protected abstract long getMaximumCompleteAge();

	/**
	 * WARNING - starts infinite loop - you have to kill 
	 * @param uri
	 * @param submitQName
	 * @param statusTName
	 * @param statusQName
	 * @throws Exception
	 */
	private void monitorSubmissionQueue(URI uri, 
										String submitQName,
										String statusTName, 
										String statusQName) throws Exception {

		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
		Connection    connection = connectionFactory.createConnection();
		Session   session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = session.createQueue(submitQName);

		final MessageConsumer consumer = session.createConsumer(queue);
		connection.start();
		
		System.out.println("Starting consumer for submissions to queue "+submitQName);
        while (isActive()) { // You have to kill it or call stop() to stop it!
            
        	try {
        		
        		// Consumes messages from the queue.
	        	Message m = consumer.receive(1000);
	            if (m!=null) {
	            	TextMessage t = (TextMessage)m;
	            	ObjectMapper mapper = new ObjectMapper();
	            	
	            	final StatusBean bean = mapper.readValue(t.getText(), getBeanClass());
	            	
                    if (bean!=null) { // We add this to the status list so that it can be rendered in the UI
                    	
                    	if (!isHandled(bean)) continue; // Consume it and move on
                    	
                    	// Now we put the bean in the status queue and we 
                    	// start the process
                    	RemoteSubmission factory = new RemoteSubmission(uri);
                    	factory.setLifeTime(t.getJMSExpiration());
                    	factory.setPriority(t.getJMSPriority());
                    	factory.setTimestamp(t.getJMSTimestamp());
                    	factory.setQueueName(statusQName); // Move the message over to a status queue.
                    	
                    	factory.submit(bean, false);
                    	
                    	final ProgressableProcess process = createProcess(uri, statusTName, statusQName, bean);
                    	if (process!=null) {
                    		process.start();
                        	System.out.println("Started job "+bean.getName()+" messageid("+t.getJMSMessageID()+")");
                    	}
                    	
                    }
	            }
        	} catch (Throwable ne) {
        		// Really basic error reporting, they have to pipe to file.
        		ne.printStackTrace();
        	}
		}
		
	}
	
	
	/**
	 * Override to stop handling certain events in the queue.
	 * @param bean
	 * @return
	 */
	protected boolean isHandled(StatusBean bean) {
		return true;
	}

	/**
	 * Parse the queue for stale jobs and things that should be rerun.
	 * @param bean
	 * @throws Exception 
	 */
	private void processStatusQueue(URI uri, String statusQName) throws Exception {
		
		QueueConnection qCon = null;
		
		try {
	 	    QueueConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
			qCon  = connectionFactory.createQueueConnection(); 
			QueueSession    qSes  = qCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue queue   = qSes.createQueue(statusQName);
			qCon.start();
			
		    QueueBrowser qb = qSes.createBrowser(queue);
		    
		    @SuppressWarnings("rawtypes")
			Enumeration  e  = qb.getEnumeration();
		    
			ObjectMapper mapper = new ObjectMapper();
			
			Map<String, StatusBean> failIds = new LinkedHashMap<String, StatusBean>(7);
			List<String>          removeIds = new ArrayList<String>(7);
	        while(e.hasMoreElements()) {
		    	Message m = (Message)e.nextElement();
		    	if (m==null) continue;
	        	if (m instanceof TextMessage) {
	            	TextMessage t = (TextMessage)m;
	              	
	            	try {
		            	@SuppressWarnings("unchecked")
						final StatusBean qbean = mapper.readValue(t.getText(), getBeanClass());
		            	if (qbean==null)               continue;
		            	if (qbean.getStatus()==null)   continue;
		            	if (!qbean.getStatus().isStarted()) {
		            		failIds.put(t.getJMSMessageID(), qbean);
		            		continue;
		            	}
		            	
		            	// If it has failed, we clear it up
		            	if (qbean.getStatus()==Status.FAILED) {
		            		removeIds.add(t.getJMSMessageID());
		            		continue;
		            	}
		            	if (qbean.getStatus()==Status.NONE) {
		            		removeIds.add(t.getJMSMessageID());
		            		continue;
		            	}
		            	
		            	// If it is running and older than a certain time, we clear it up
		            	if (qbean.getStatus()==Status.RUNNING) {
		            		final long submitted = qbean.getSubmissionTime();
		            		final long current   = System.currentTimeMillis();
		            		if (current-submitted > getMaximumRunningAge()) {
		            			removeIds.add(t.getJMSMessageID());
		            			continue;
		            		}
		            	}
		            	
		            	if (qbean.getStatus().isFinal()) {
		            		final long submitted = qbean.getSubmissionTime();
		            		final long current   = System.currentTimeMillis();
		            		if (current-submitted > getMaximumCompleteAge()) {
		            			removeIds.add(t.getJMSMessageID());
		            		}
		            	}

	            	} catch (Exception ne) {
	            		System.out.println("Message "+t.getText()+" is not legal and will be removed.");
	            		removeIds.add(t.getJMSMessageID());
	            	}
	        	}
		    }
	        
	        // We fail the non-started jobs now - otherwise we could
	        // actually start them late. TODO check this
        	final List<String> ids = new ArrayList<String>();
        	ids.addAll(failIds.keySet());
        	ids.addAll(removeIds);
        	
	        if (ids.size()>0) {
	        	
	        	for (String jMSMessageID : ids) {
		        	MessageConsumer consumer = qSes.createConsumer(queue, "JMSMessageID = '"+jMSMessageID+"'");
		        	Message m = consumer.receive(1000);
		        	if (removeIds.contains(jMSMessageID)) continue; // We are done
		        	
		        	if (m!=null && m instanceof TextMessage) {
		        		MessageProducer producer = qSes.createProducer(queue);
		        		final StatusBean    bean = failIds.get(jMSMessageID);
		        		bean.setStatus(Status.FAILED);
		        		producer.send(qSes.createTextMessage(mapper.writeValueAsString(bean)));
		        		
                    	System.out.println("Failed job "+bean.getName()+" messageid("+jMSMessageID+")");

		        	}
				}
	        }
		} finally {
			if (qCon!=null) qCon.close();
		}
		
	}


}
