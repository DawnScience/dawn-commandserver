package org.dawnsci.commandserver.core;

import org.eclipse.scanning.api.event.IEventConnectorService;

public class ActiveMQServiceHolder {

	private static IEventConnectorService eventConnectorService;

	public static IEventConnectorService getEventConnectorService() {
		return eventConnectorService;
	}

	public static void setEventConnectorService(IEventConnectorService eventService) {
		ActiveMQServiceHolder.eventConnectorService = eventService;
	}
}
