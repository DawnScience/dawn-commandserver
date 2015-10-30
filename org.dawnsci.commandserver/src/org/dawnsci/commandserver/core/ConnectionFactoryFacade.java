/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.core;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;

import org.eclipse.scanning.api.event.IEventConnectorService;

/**
 * Class exists to avoid dependency on org.apache.activemq leaking around the code
 * base. Please use this facade to keep things modular.
 * 
 * @author Matthew Gerring
 *
 */
public class ConnectionFactoryFacade {

	/**
	 * Create a ConnectionFactory using activemq
	 * @param uri
	 * @return
	 * @throws JMSException
	 * @throws URISyntaxException 
	 */
	public static QueueConnectionFactory createConnectionFactory(final String uri) throws JMSException, URISyntaxException {
		return createConnectionFactory(new URI(uri));
	}

	/**
	 * Create a ConnectionFactory using activemq
	 * @param uri
	 * @return
	 * @throws JMSException
	 */
	public static QueueConnectionFactory createConnectionFactory(final URI uri) throws JMSException {
        final IEventConnectorService service = ActiveMQServiceHolder.getEventConnectorService();
		return (QueueConnectionFactory)service.createConnectionFactory(uri);
	}
}
