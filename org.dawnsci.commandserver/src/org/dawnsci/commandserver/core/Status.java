package org.dawnsci.commandserver.core;

/**
 * States of jobs on the cluster.
 * 
 * @author fcp94556
 *
 */
public enum Status {

	SUBMITTED, QUEUED, RUNNING, CANCELLED, FAILED, COMPLETE;

	/**
	 * 
	 * @return true if the run was taken from the queue and something was actually executed on it.
	 */
	public boolean isStarted() {
		return this!=SUBMITTED;
	}
}
