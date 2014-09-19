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

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;

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
	public DummyProcess(final URI   uri, 
			            final String   statusTName, 
			            final String   statusQName, 
                        StatusBean     bean) {	
		super(uri, statusTName, statusQName, bean);
	}


	@Override
	public void execute() throws Exception {
		
        bean.setStatus(Status.RUNNING);
        bean.setPercentComplete(1);
        broadcast(bean);
		
        dryRun();
	}


	@Override
	public void terminate() throws Exception {
		// We do nothing here, normally the dryRun() will now exist because
		// the status changed.
	}

}
