/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.mx.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.dawnsci.commandserver.core.server.FilePermissionServer;
import org.dawnsci.commandserver.mx.beans.ProjectBean;
import org.dawnsci.commandserver.mx.process.Xia2Process;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * Example commissioning run: /dls/i03/data/2014/cm4950-2/20140425/gw/processing/thau1
 * 
 * @author Matthew Gerring
 *
 */
public class MXSubmissionConsumer extends ProcessConsumer {

	public final static String NAME = "Multi-crystal Reprocessing Consumer";
	
	private FilePermissionServer server;
	public MXSubmissionConsumer() {
		consumerVersion = "1.1";
	}

	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public void start() throws Exception {
		
		if (config.containsKey("validatorport")) {
			// We start a validator on this machine using Jetty
			// This allows directory paths to be checked for existence and 
			// if they are writable to the consumer.
			this.server = new FilePermissionServer();
			server.setPort(Integer.parseInt(config.get("validatorport")));
			server.start();
		}
		
		// Blocking
		super.start();
	}
	
	@Override
	public void stop() throws Exception {
		super.stop();
		if (server!=null) server.stop();
	}

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return ProjectBean.class;
	}
	
	@Override
	protected ProgressableProcess createProcess(URI uri, 
			                                    String statusTName,
			                                    String statusQName, 
			                                    StatusBean bean) throws Exception {

		return new Xia2Process(uri, statusTName, statusQName, config, (ProjectBean)bean);
	}

	
	private static final long TWO_DAYS = 48*60*60*1000; // ms
	
	@Override
	protected long getMaximumRunningAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumXia2RunningAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumXia2RunningAge"));
		}
		return TWO_DAYS;
	}
	
	private static final long A_WEEK = 7*24*60*60*1000; // ms
	
	@Override
	protected long getMaximumCompleteAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumXia2CompleteAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumXia2CompleteAge"));
		}
		return A_WEEK;
	}


}
