/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.core.consumer;

/**
 * Status for consumers such that a client may connect to activemq and see diamond consumers
 * @author fcp94556
 *
 */
public enum ConsumerStatus {

	STARTING, STOPPING, RUNNING, NOT_AVAILABLE, STOPPED, REQUEST_TERMINATE;

	public boolean isFinal() {
		return this==NOT_AVAILABLE || this==STOPPED;
	}
}
