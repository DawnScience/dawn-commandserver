package org.dawnsci.commandserver.core;

import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.IEventService;

public class ActiveMQServiceHolder {

	private static IEventConnectorService eventConnectorService;
	private static IEventService          eventService;

	public static IEventConnectorService getEventConnectorService() {
		return eventConnectorService;
	}

	public void setEventConnectorService(IEventConnectorService eventService) {
		ActiveMQServiceHolder.eventConnectorService = eventService;
	}

	public static IEventService getEventService() {
		return eventService;
	}

	public void setEventService(IEventService eventService) {
		ActiveMQServiceHolder.eventService = eventService;
	}
}
