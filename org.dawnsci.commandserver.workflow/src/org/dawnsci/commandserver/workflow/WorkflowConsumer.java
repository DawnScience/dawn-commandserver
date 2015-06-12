package org.dawnsci.commandserver.workflow;

import java.io.File;
import java.net.URI;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.dawnsci.commandserver.foldermonitor.EventType;
import org.dawnsci.commandserver.foldermonitor.FolderEventBean;

public class WorkflowConsumer extends ProcessConsumer {

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
  	    return StatusBean.class;
	}

	@Override
	protected boolean isHandled(StatusBean bean) {
		
		if (bean instanceof FolderEventBean) {
			FolderEventBean feb = (FolderEventBean)bean;
			if (feb.getType()!=EventType.ENTRY_CREATE) return false; // Only interested in new files.
		}
		return true;
	}

	@Override
	protected ProgressableProcess createProcess(URI uri,
			                                    String statusTName,
			                                    String statusQName, 
			                                    StatusBean bean) throws Exception {
		// We are only interested in new files
		if (bean instanceof FolderEventBean) {
			FolderEventBean feb = (FolderEventBean)bean;
			if (feb.getType()!=EventType.ENTRY_CREATE) return null; // Only interested in new files.
		}
				
		WorkflowProcess process = new WorkflowProcess(uri, config.get("processName"), statusTName, statusQName, config, bean);
		if (config.containsKey("blocking")) {
			process.setBlocking(Boolean.parseBoolean(config.get("blocking")));
		} else {
			process.setBlocking(false); // No blocking
		}
		
		return process;
	}

	@Override
	public String getName() {
		if (config.containsKey("consumerName")) return config.get("consumerName");
		return getClass().getSimpleName();
	}

}
