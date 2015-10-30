package org.dawnsci.commandserver.workflow;

import java.io.File;
import java.net.URI;

import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.eclipse.scanning.api.event.status.StatusBean;

public class WorkflowConsumer extends ProcessConsumer {

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
  	    return StatusBean.class;
	}

	@Override
	protected boolean isHandled(StatusBean bean) {
		
		if (bean.getProperties().containsKey("event_type")) {
			final String type = bean.getProperty("event_type");
			if (!type.equals("ENTRY_CREATE")) return false; // Only interested in new files.
		}
		return true;
	}

	@Override
	protected ProgressableProcess createProcess(URI uri,
			                                    String statusTName,
			                                    String statusQName, 
			                                    StatusBean bean) throws Exception {
		// We are only interested in new files
		if (bean.getProperties().containsKey("event_type")) {
			final String type = bean.getProperty("event_type");
			if (!type.equals("ENTRY_CREATE")) return null; // Only interested in new files.
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
		
		final StringBuilder buf = new StringBuilder();
		if (config.containsKey("consumerName")) {
			buf.append( config.get("consumerName") );
		} else {
		    buf.append(getClass().getSimpleName());
		}
		String momlLocation = config.get("momlLocation");
		if (momlLocation==null || "null".equals(momlLocation)) throw new RuntimeException("-momlLocation argument must be set");
		final File moml = new File(momlLocation);
		buf.append(" (");
		buf.append(moml.getName());
		buf.append(" )");
        return buf.toString();
	}

}
