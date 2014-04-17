package org.dawnsci.commandserver.mx.consumer;

import org.dawnsci.commandserver.core.Status;
import org.dawnsci.commandserver.mx.beans.DataCollectionsBean;

public abstract class RunningProcess {

	protected final DataCollectionsBean bean;

	public RunningProcess(DataCollectionsBean bean) {
		this.bean = bean;
		bean.setStatus(Status.QUEUED);
		sendBean(bean);
	}


	private void sendBean(DataCollectionsBean bean) {
		// TODO Auto-generated method stub
		
	}

	
	public abstract void start();
}
