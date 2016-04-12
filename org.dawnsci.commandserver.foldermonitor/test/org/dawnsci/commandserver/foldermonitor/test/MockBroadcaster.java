package org.dawnsci.commandserver.foldermonitor.test;

import java.io.PrintStream;
import java.net.URI;
import java.util.UUID;

import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventConnectorService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.StatusBean;

class MockBroadcaster implements IPublisher<StatusBean> {

	private StatusBroadcastListener listener;

	public MockBroadcaster(StatusBroadcastListener l) {
	    this.listener = l;
	}

	/**
	 * Mocks out the broadcast to redefine what happens because we are running a test.
	 */
	public void broadcast(StatusBean bean) throws EventException {
		listener.statusBroadcasted(bean);
	}
	
	public void dispose() {
		// Nothing to dispose
	}

	@Override
	public String getTopicName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setTopicName(String topic) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public URI getUri() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAlive(boolean alive) throws EventException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isAlive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setQueueName(String queueName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getQueueName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConsumerName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setConsumerName(String cname) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public UUID getConsumerId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setConsumerId(UUID id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLoggingStream(PrintStream stream) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IEventConnectorService getConnectorService() {
		return null;
	}
}
