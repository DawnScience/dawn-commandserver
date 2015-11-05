package org.dawnsci.commandserver.bubbles;

import java.net.URI;

import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.StatusBean;

public class BubblesConsumer extends ProcessConsumer<BubblesBean> {

	@Override
	protected Class<BubblesBean> getBeanClass() {
		return BubblesBean.class;
	}

	@Override
	protected ProgressableProcess createProcess(BubblesBean bean, IPublisher<BubblesBean> publisher) throws Exception {
		return new BubblesProcess(bean, publisher);
	}

	@Override
	public String getName() {
		return "Bubbles Consumer";
	}

}
