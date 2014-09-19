/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.jython;

import java.net.URI;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;

public class JythonConsumer extends ProcessConsumer {

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return JythonBean.class;
	}

	@Override
	protected ProgressableProcess createProcess(URI uri, String statusTName, String statusQName, StatusBean bean) throws Exception {
		return new JythonProcess(uri, statusTName, statusQName, config, (JythonBean)bean);
	}

	private static final long TWO_DAYS = 48*60*60*1000; // ms
	
	@Override
	protected long getMaximumRunningAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumJythonRunningAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumJythonRunningAge"));
		}
		return TWO_DAYS;
	}
	
	private static final long A_WEEK = 7*24*60*60*1000; // ms
	
	@Override
	protected long getMaximumCompleteAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumJythonCompleteAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumJythonCompleteAge"));
		}
		return A_WEEK;
	}

	@Override
	public String getName() {
		return "Jython VM";
	}

}
