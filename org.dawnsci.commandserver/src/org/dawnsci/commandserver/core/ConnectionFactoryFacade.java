package org.dawnsci.commandserver.core;

import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Class exists to avoid dependency on org.apache.activemq leaking around the code
 * base. Please use this facade to keep things modular.
 * 
 * @author fcp94556
 *
 */
public class ConnectionFactoryFacade {

	/**
	 * Create a ConnectionFactory using activemq
	 * @param uri
	 * @return
	 * @throws JMSException
	 */
	public static QueueConnectionFactory createConnectionFactory(final String uri) throws JMSException {
		return new ActiveMQConnectionFactory(uri);
	}
}
