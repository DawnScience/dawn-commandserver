package org.dawnsci.commandserver.mx.dummy;

import org.dawnsci.commandserver.mx.beans.DataCollectionsBean;
import org.dawnsci.commandserver.mx.consumer.RunningProcess;

/**
 * 
 * This process behaves as if some analysis was being run on the server.
 * 
 * It sends %-complete for the process back to the status queue.
 * 
 * @author fcp94556
 *
 */
public class DummyProcess extends RunningProcess{

	public DummyProcess(DataCollectionsBean bean) {
		
		super(bean);
		
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		
	}

}
