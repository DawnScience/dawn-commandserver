package org.dawnsci.commandserver.bubbles;

import java.net.URI;

import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.eclipse.scanning.api.event.status.StatusBean;

public class BubblesConsumer extends ProcessConsumer {

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return BubblesBean.class;
	}

	@Override
	protected ProgressableProcess createProcess(URI        uri,
			                                    String     statusTName,
			                                    String     statusQName, 
			                                    StatusBean bean) throws Exception {
		return new BubblesProcess(uri, statusTName, statusQName, bean);
	}

	@Override
	public String getName() {
		return "Bubbles Consumer";
	}

}
