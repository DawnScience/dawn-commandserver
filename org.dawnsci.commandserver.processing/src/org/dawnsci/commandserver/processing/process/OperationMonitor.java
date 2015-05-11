package org.dawnsci.commandserver.processing.process;

import org.dawnsci.commandserver.core.process.IBroadcaster;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.monitor.IMonitor;

public class OperationMonitor implements IMonitor {

	private OperationBean obean;
	private IBroadcaster  broadcaster;
	private int           total;
	private int           count;
	
	public OperationMonitor(OperationBean obean, IBroadcaster broadcaster, int total) throws Exception {
		this.obean       = obean;
		this.broadcaster = broadcaster;
		this.total       = total;
	}
	
	@Override
	public void worked(int amount) {
		count+=amount;
		double done = (double)count / (double)total;
		obean.setPercentComplete(done);
		broadcaster.broadcast(obean);	

	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public void subTask(String taskName) {
	}

}
