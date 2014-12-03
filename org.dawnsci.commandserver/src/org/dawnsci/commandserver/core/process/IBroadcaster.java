package org.dawnsci.commandserver.core.process;

import org.dawnsci.commandserver.core.beans.StatusBean;

/**
 * Broadcast a bean, usually over a JMS topic.
 * 
 * @author fcp94556
 *
 */
public interface IBroadcaster {

	/**
	 * Called to publish the bean on a topic.
	 * @param bean
	 */
	void broadcast(StatusBean bean);
}
