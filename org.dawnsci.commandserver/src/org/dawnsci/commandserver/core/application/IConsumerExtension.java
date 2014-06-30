package org.dawnsci.commandserver.core.application;

import java.util.Map;

/**
 * Interface to encapsulate something which can be created and started 
 * and will then send activemq messages when it is going.
 * 
 * @author fcp94556
 *
 */
public interface IConsumerExtension {

	/**
	 * Command line arguments which started the consumer
	 * @param configuration
	 * @throws Exception
	 */
	public void init(Map<String, String> configuration) throws Exception;

	/**
	 * Start the worker, listen to queues and other resources.
	 * @throws Exception
	 */
	public void start() throws Exception;

	/**
	 * Stop the worker, releasing any resources
	 * @throws Exception
	 */
	public void stop() throws Exception;

}
