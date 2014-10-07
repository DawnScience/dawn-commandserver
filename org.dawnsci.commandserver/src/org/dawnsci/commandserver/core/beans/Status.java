package org.dawnsci.commandserver.core.beans;

/**
 * States of jobs on the cluster.
 * 
 * @author fcp94556
 *
 */
public enum Status {

	SUBMITTED, QUEUED, RUNNING, TERMINATED, REQUEST_TERMINATE, FAILED, COMPLETE, UNFINISHED, NONE;

	/**
	 * 
	 * @return true if the run was taken from the queue and something was actually executed on it.
	 */
	public boolean isStarted() {
		return this!=SUBMITTED;
	}
	
	public boolean isFinal() {
		return this==TERMINATED || this==FAILED || this==COMPLETE || this==UNFINISHED || this==NONE;
	}
}
