package org.dawnsci.commandserver.core;

/**
 * States of jobs on the cluster.
 * 
 * @author fcp94556
 *
 */
public enum Status {

	SUBMITTED, QUEUED, RUNNING, CANCELLED, FAILED;
}
