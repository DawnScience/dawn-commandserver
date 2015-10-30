package org.dawnsci.commandserver.foldermonitor.test;

import org.dawnsci.commandserver.core.producer.Broadcaster;
import org.eclipse.scanning.api.event.status.StatusBean;

class MockBroadcaster extends Broadcaster {

	private StatusBroadcastListener listener;

	public MockBroadcaster(StatusBroadcastListener l) {
		super(null, null, null);
	    this.listener = l;
	}

	/**
	 * Mocks out the broadcast to redefine what happens because we are running a test.
	 */
	public void broadcast(StatusBean bean, boolean add) throws Exception {
		listener.statusBroadcasted(bean);
	}
	
	public void dispose() {
		// Nothing to dispose
	}
}
