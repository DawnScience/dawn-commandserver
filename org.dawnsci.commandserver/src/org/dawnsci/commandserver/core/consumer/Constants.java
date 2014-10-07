/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.core.consumer;

public class Constants {

	/**
	 * Topic on which consumers publish to say that they are started up, shutdown and otherwise still alive
	 */
	public static final String ALIVE_TOPIC = "scisoft.commandserver.core.ALIVE_TOPIC";
	
	/**
	 * Topic on which consumers publish to say that they are started up, shutdown and otherwise still alive
	 */
	public static final String ADMIN_MESSAGE_TOPIC = "scisoft.commandserver.core.ADMINISTRATOR_MESSAGE";

	/**
	 * Topic on which consumers publish to say that they are started up, shutdown and otherwise still alive
	 */
	public static final String TERMINATE_CONSUMER_TOPIC = "scisoft.commandserver.core.TERMINATE_TOPIC";

	/**
	 * Rate at which consumers tell the world that they are still alive.
	 */
	public static final long   NOTIFICATION_FREQUENCY = 2000; // ms 
}
