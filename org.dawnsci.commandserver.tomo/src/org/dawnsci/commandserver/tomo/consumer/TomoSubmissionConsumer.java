/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.tomo.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.process.ProcessConsumer;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.tomo.beans.TomoBean;
import org.dawnsci.commandserver.tomo.process.TomoProcess;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.StatusBean;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * @author Matthew Gerring
 *
 */
public class TomoSubmissionConsumer extends ProcessConsumer<TomoBean> {


	@Override
	public String getName() {
		return "Tomography Reconstruction Consumer";
	}

	@Override
	protected Class<TomoBean> getBeanClass() {
		return TomoBean.class;
	}


	@Override
	protected ProgressableProcess<TomoBean> createProcess(TomoBean bean, IPublisher<TomoBean> status) throws Exception {

		return new TomoProcess(bean, status);
	}

}
