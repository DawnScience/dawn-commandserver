package org.dawnsci.commandserver.bubbles;

import org.dawnsci.commandserver.core.process.AbstractProcessConsumer;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.eclipse.scanning.api.event.core.IPublisher;

public class BubblesConsumer extends AbstractProcessConsumer<BubblesBean> {

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
