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

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.dawnsci.commandserver.tomo.beans.TomoBean;
import org.dawnsci.commandserver.tomo.process.TomoProcess;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * @author Matthew Gerring
 *
 */
public class TomoSubmissionConsumer extends ProcessConsumer {


	@Override
	public String getName() {
		return "Tomography Reconstruction Consumer";
	}

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return TomoBean.class;
	}


	@Override
	protected ProgressableProcess createProcess(URI uri, 
			                                    String statusTName,
			                                    String statusQName, 
			                                    StatusBean bean) throws Exception {

		return new TomoProcess(uri, statusTName, statusQName, (TomoBean)bean);
	}
		
	@Override
	protected long getMaximumRunningAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumTomoRunningAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumTomoRunningAge"));
		}
		return TWO_DAYS;
	}
		
	@Override
	protected long getMaximumCompleteAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumXia2CompleteAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumTomoCompleteAge"));
		}
		return A_WEEK;
	}


}
