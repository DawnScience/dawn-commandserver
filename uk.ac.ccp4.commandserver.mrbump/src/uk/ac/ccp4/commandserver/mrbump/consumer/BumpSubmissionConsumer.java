/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.ccp4.commandserver.mrbump.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;

import uk.ac.ccp4.commandserver.mrbump.beans.BumpBean;
import uk.ac.ccp4.commandserver.mrbump.process.BumpProcess;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * @author Matthew Gerring
 *
 */
public class BumpSubmissionConsumer extends ProcessConsumer {


	@Override
	public String getName() {
		return "Mr Bump Consumer";
	}

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return BumpBean.class;
	}


	@Override
	protected ProgressableProcess createProcess(URI uri, 
			                                    String statusTName,
			                                    String statusQName, 
			                                    StatusBean bean) throws Exception {

		return new BumpProcess(uri, statusTName, statusQName, (BumpBean)bean);
	}
		
	@Override
	protected long getMaximumRunningAge() {
		if (System.getProperty("uk.ac.ccp4.commandserver.mrbump.consumer.maximumTomoRunningAge")!=null) {
			return Long.parseLong(System.getProperty("uk.ac.ccp4.commandserver.mrbump.consumer.maximumTomoRunningAge"));
		}
		return TWO_DAYS;
	}
		
	@Override
	protected long getMaximumCompleteAge() {
		if (System.getProperty("uk.ac.ccp4.commandserver.mrbump.consumer.maximumXia2CompleteAge")!=null) {
			return Long.parseLong(System.getProperty("uk.ac.ccp4.commandserver.mrbump.consumer.maximumTomoCompleteAge"));
		}
		return A_WEEK;
	}


}
