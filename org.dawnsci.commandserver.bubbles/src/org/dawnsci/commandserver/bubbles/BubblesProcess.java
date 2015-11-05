package org.dawnsci.commandserver.bubbles;

import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.Status;

public class BubblesProcess extends ProgressableProcess<BubblesBean> {

	public BubblesProcess(BubblesBean bean, IPublisher<BubblesBean> status) {
		super(bean, status, true);
	}

	@Override
	public void execute() throws EventException {
        bean.setStatus(Status.RUNNING);
        bean.setPercentComplete(1);
        broadcast(bean);
		
        dryRun();
	}

	@Override
	public void terminate() throws EventException {
		throw new EventException("Terminate not implemented!");
	}

}
