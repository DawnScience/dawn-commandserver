package org.dawnsci.commandserver.core.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;

/**
 * 
 * This process behaves as if some analysis was being run on the server.
 * 
 * It sends %-complete for the process back to the status queue.
 * 
 * @author fcp94556
 *
 */
public class DummyProcess extends ProgressableProcess {

	/**
	 * 
	 * 
	 * @param uri
	 * @param topicName
	 * @param queuedMessage
	 * @param bean
	 */
	public DummyProcess(final URI   uri, 
			            final String   statusTName, 
			            final String   statusQName, 
                        StatusBean     bean) {	
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
	protected void terminate() throws Exception {
		// We do nothing here, normally the dryRun() will now exist because
		// the status changed.
	}

}
