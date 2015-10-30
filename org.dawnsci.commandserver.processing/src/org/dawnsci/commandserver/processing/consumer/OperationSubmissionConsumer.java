/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.processing.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.dawnsci.commandserver.processing.process.OperationProcess;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * @author Matthew Gerring
 *
 */
public class OperationSubmissionConsumer extends ProcessConsumer {


	@Override
	public String getName() {
		return "Operation Pipeline Consumer";
	}

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return OperationBean.class;
	}


	@Override
	protected ProgressableProcess createProcess(URI uri, 
			                                    String statusTName,
			                                    String statusQName, 
			                                    StatusBean bean) throws Exception {

		return new OperationProcess(uri, statusTName, statusQName, config, (OperationBean)bean);
	}


}
