package org.dawnsci.commandserver.mx.dummy;

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
	public DummyProcess(final String   uri, 
			            final String   statusTName, 
			            final String   statusQName, 
                        StatusBean     bean) {	
		super(uri, statusTName, statusQName, bean);
	}


	@Override
	public void run() {
		
        bean.setStatus(Status.RUNNING);
        bean.setPercentComplete(1);
        broadcast(bean);
		
        for (int i = 0; i < 100; i++) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			bean.setPercentComplete(i);
	        broadcast(bean);
		}
        
        bean.setStatus(Status.COMPLETE);
        bean.setPercentComplete(100);
        broadcast(bean);
	}

}
