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
