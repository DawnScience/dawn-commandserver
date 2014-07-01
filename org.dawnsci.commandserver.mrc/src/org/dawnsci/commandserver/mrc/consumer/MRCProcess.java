package org.dawnsci.commandserver.mrc.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;

/**
 * A process which blocks until done.
 * @author fcp94556
 *
 */
public class MRCProcess extends ProgressableProcess {

	public MRCProcess(URI uri, String statusTName, String statusQName, StatusBean bean) {
		
		super(uri, statusTName, statusQName, bean);
	}

	@Override
	protected void execute() throws Exception {
		
        dryRun(100); // Notifies events over 10s
	}

	@Override
	protected void terminate() throws Exception {
		
		// Interrupt the sleeping
		Thread.currentThread().interrupt();
	}

}
