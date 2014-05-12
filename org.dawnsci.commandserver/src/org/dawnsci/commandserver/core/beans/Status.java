package org.dawnsci.commandserver.core.beans;

/**
 * States of jobs on the cluster.
 * 
 * @author fcp94556
 *
 */
public enum Status {

	SUBMITTED, QUEUED, RUNNING, CANCELLED, REQUEST_TERMINATE, FAILED, COMPLETE, NONE;

	/**
	 * 
	 * @return true if the run was taken from the queue and something was actually executed on it.
	 */
	public boolean isStarted() {
		return this!=SUBMITTED;
	}
	
	public boolean isFinal() {
		return this==CANCELLED || this==FAILED || this==COMPLETE || this==NONE;
	}
}
