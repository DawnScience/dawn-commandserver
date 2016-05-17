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

import org.dawnsci.commandserver.core.process.ProcessConsumer;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.processing.process.OperationProcess;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.StatusBean;

import uk.ac.diamond.scisoft.analysis.processing.bean.OperationBean;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * @author Matthew Gerring
 *
 */
public class OperationSubmissionConsumer extends ProcessConsumer<OperationBean> {


	@Override
	public String getName() {
		return "Operation Pipeline Consumer";
	}

	@Override
	protected Class<OperationBean> getBeanClass() {
		return OperationBean.class;
	}


	@Override
	protected ProgressableProcess<OperationBean> createProcess(OperationBean bean, IPublisher<OperationBean> status) throws Exception {

		return new OperationProcess(bean, status);
	}


}
