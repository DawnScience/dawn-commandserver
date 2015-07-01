package org.dawnsci.commandserver.bubbles;

import java.net.URI;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;

public class BubblesProcess extends ProgressableProcess {

	public BubblesProcess(final URI uri, final String statusTName, final String statusQName, StatusBean bean) {
		super(uri, statusTName, statusQName, bean);
	}

	@Override
	public void execute() throws Exception {
        bean.setStatus(Status.RUNNING);
        bean.setPercentComplete(1);
        broadcast(bean);
		
        dryRun();
	}

	@Override
	public void terminate() throws Exception {
		// TODO Auto-generated method stub

	}

}
