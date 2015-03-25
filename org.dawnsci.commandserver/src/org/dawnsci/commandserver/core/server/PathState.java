package org.dawnsci.commandserver.core.server;

public enum PathState {

	OK, INVALID, NON_EXISTING, NON_READABLE, NON_WRITABLE;
	
	public boolean isOk() {
		return this==OK;
	}
}
