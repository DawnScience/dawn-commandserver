/*
 * Copyright (c) 2014 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.ptycho.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.dawnsci.commandserver.ptycho.beans.PtychoBean;
import org.dawnsci.commandserver.ptycho.process.PtychoProcess;

/**
 * This consumer monitors a queue and starts runs based on what is submitted.
 * 
 * 
 */
public class PtychoSubmissionConsumer extends ProcessConsumer {

	@Override
	public String getName() {
		return "Ptychography Reconstruction Consumer";
	}

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return PtychoBean.class;
	}

	@Override
	protected ProgressableProcess createProcess(URI uri, String statusTName,
			String statusQName, StatusBean bean) throws Exception {

		return new PtychoProcess(uri, statusTName, statusQName,
				(PtychoBean) bean);
	}

	@Override
	protected long getMaximumRunningAge() {
		if (System
				.getProperty("org.dawnsci.commandserver.core.maximumPtychoRunningAge") != null) {
			return Long
					.parseLong(System
							.getProperty("org.dawnsci.commandserver.core.maximumPtychoRunningAge"));
		}
		return TWO_DAYS;
	}

	@Override
	protected long getMaximumCompleteAge() {
		if (System
				.getProperty("org.dawnsci.commandserver.core.maximumPtychoCompleteAge") != null) {
			return Long
					.parseLong(System
							.getProperty("org.dawnsci.commandserver.core.maximumPtychoCompleteAge"));
		}
		return A_WEEK;
	}

}
