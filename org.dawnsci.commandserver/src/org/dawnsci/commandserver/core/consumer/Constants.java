package org.dawnsci.commandserver.core.consumer;

public class Constants {

	/**
	 * Topic on which consumers publish to say that they are started up, shutdown and otherwise still alive
	 */
	public static final String ALIVE_TOPIC = "scisoft.commandserver.core.ALIVE_TOPIC";
	
	/**
	 * Topic on which consumers publish to say that they are started up, shutdown and otherwise still alive
	 */
	public static final String TERMINATE_CONSUMER_TOPIC = "scisoft.commandserver.core.TERMINATE_TOPIC";

	/**
	 * Rate at which consumers tell the world that they are still alive.
	 */
	public static final long   NOTIFICATION_FREQUENCY = 2000; // ms 
}
