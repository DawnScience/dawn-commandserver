/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.core.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * 
 * This process behaves as if some analysis was being run on the server.
 * 
 * It sends %-complete for the process back to the status queue.
 * 
 * @author Matthew Gerring
 *
 */
public class DummyProcess extends ProgressableProcess {

	/**
	 * 
	 * 
	 * @param uri
	 * @param topicName
	 * @param queuedMessage
	 * @param bean
	 */
	public DummyProcess(StatusBean bean, IPublisher<StatusBean> statusPublisher) {	
		super(bean, statusPublisher, false);
	}


	@Override
	public void execute() throws EventException {
		
        bean.setStatus(Status.RUNNING);
        bean.setPercentComplete(1);
        broadcast(bean);
		
        dryRun();
	}


	@Override
	public void terminate() throws EventException {
		// We do nothing here, normally the dryRun() will now exit because
		// the status changed.
	}

}
