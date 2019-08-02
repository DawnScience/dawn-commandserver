/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.core.process;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.dawnsci.commandserver.core.application.IConsumerExtension;
import org.eclipse.scanning.api.event.EventConstants;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IJobQueue;
import org.eclipse.scanning.api.event.core.IBeanProcess;
import org.eclipse.scanning.api.event.core.IProcessCreator;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * Please extend this consumer to create it and call the start method.
 * 
 * You must have the no argument constructor because the org.dawnsci.commandserver.core.application.Consumer
 * application requires this to start and stop the consumer.
 * 
 * @author Matthew Gerring
 *
 */
public abstract class AbstractProcessConsumer<T extends StatusBean> implements IConsumerExtension {

	private String submitQueueName;
	private String statusTopicName;
	protected Map<String, String> config;

	protected boolean durable = true;
	protected URI uri;
	
	private IJobQueue<T> jobQueue;
	protected String consumerVersion;
	
	public AbstractProcessConsumer() {
		this.consumerVersion = "1.0";
	}
	
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
		this.submitQueueName = config.get("submit");
		this.statusTopicName = config.get("topic");
	}

	/**
	 * Starts the consumer and does not return.
	 * @throws Exception
	 */
	public void start() throws Exception {
		
		IEventService service = ActiveMQServiceHolder.getEventService();
		this.jobQueue = service.createJobQueue(uri, submitQueueName, statusTopicName, EventConstants.QUEUE_STATUS_TOPIC, EventConstants.CMD_TOPIC, EventConstants.ACK_TOPIC);
		jobQueue.setRunner(new IProcessCreator<T>() {
			@Override
			public IBeanProcess<T> createProcess(T bean, IPublisher<T> publisher) throws EventException {
				try {
					ProgressableProcess<T> process = AbstractProcessConsumer.this.createProcess(bean, publisher);
					process.setArguments(config);
					return process;
				} catch (Exception ne) {
					throw new EventException("Problem creating process!", ne);
				}
			}
		});
		jobQueue.setName(getName());
		jobQueue.cleanUpCompleted();
		jobQueue.setBeanClass(getBeanClass());
		// This is the blocker
		jobQueue.run();
	}
	
	/**
	 * You may override this method to stop the consumer cleanly. Please
	 * call super.stop() if you do.
	 * @throws JMSException 
	 */
	public void stop() throws Exception {
		jobQueue.stop();
		jobQueue.disconnect();
	}

	/**
	 * Implement to return the actual bean class in the queue
	 * @return
	 */
	protected abstract Class<T> getBeanClass();
	
	/**
	 * Implement to create the required command server process.
	 * 
	 * @param uri
	 * @param statusTopicName
	 * @param statusQueueName
	 * @param bean
	 * @return the process or null if the message should be consumed and nothing done.
	 */
	protected abstract ProgressableProcess<T> createProcess(T bean, IPublisher<T> publisher) throws Exception;
	
	// TODO FIXME
	protected volatile int processCount;


	/**
	 * Override to stop handling certain events in the queue.
	 * @param bean
	 * @return
	 */
	protected boolean isHandled(StatusBean bean) {
		return true;
	}
		
	protected static final long TWO_DAYS = 48*60*60*1000l; // ms
	protected static final long A_WEEK   = 7*24*60*60*1000l; // ms

	/**
	 * Defines the time in ms that a job may be in the running state
	 * before the consumer might consider it for deletion. If a consumer
	 * is restarted it will normally delete old running jobs older than 
	 * this age.
	 * 
	 * @return
	 */
	protected long getMaximumRunningAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumRunningAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumRunningAge"));
		}
		return TWO_DAYS;
	}
		
	/**
	 * Defines the time in ms that a job may be in the complete (or other final) state
	 * before the consumer might consider it for deletion. If a consumer
	 * is restarted it will normally delete old complete jobs older than 
	 * this age.
	 * 
	 * @return
	 */
	protected long getMaximumCompleteAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumCompleteAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumCompleteAge"));
		}
		return A_WEEK;
	}

	public boolean isDurable() {
		return durable;
	}

	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public String getConsumerVersion() {
		return consumerVersion;
	}

	public void setConsumerVersion(String consumerVersion) {
		this.consumerVersion = consumerVersion;
	}

}
