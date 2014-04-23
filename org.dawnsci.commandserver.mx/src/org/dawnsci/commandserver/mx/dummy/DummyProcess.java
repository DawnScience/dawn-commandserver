package org.dawnsci.commandserver.mx.dummy;

import javax.jms.TextMessage;

import org.dawnsci.commandserver.core.ProgressableProcess;
import org.dawnsci.commandserver.core.Status;
import org.dawnsci.commandserver.mx.beans.DataCollectionsBean;

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
                        final String   topicName, 
                        TextMessage    queuedMessage, 
                        DataCollectionsBean bean) {	
		super(uri, topicName, queuedMessage, bean);
	}


	@Override
	public void run() {
		
        bean.setStatus(Status.RUNNING);
        bean.setPercentComplete(1);
        broadcast(bean);
		
        for (int i = 0; i < 100; i++) {
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			bean.setPercentComplete(i);
	        broadcast(bean);
		}
	}

}
