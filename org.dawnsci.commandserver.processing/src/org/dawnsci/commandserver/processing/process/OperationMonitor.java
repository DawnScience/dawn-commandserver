package org.dawnsci.commandserver.processing.process;

import java.net.URI;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.eclipse.january.IMonitor;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.status.Status;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.processing.bean.OperationBean;

/**
 * Simply logs the message returned from the operations.
 * 
 * @author Matthew Gerring
 *
 */
public class OperationMonitor implements IMonitor {
	
	private static final Logger logger = LoggerFactory.getLogger(OperationMonitor.class);

	private OperationBean obean;
	private int           total;
	private int           count;
	private boolean       cancelled;
	private IPublisher<OperationBean> publisher; 
	
	public OperationMonitor(OperationBean obean, int total) {
		this.obean       = obean;
		this.total       = total;
		try {
			IEventService eventService = ActiveMQServiceHolder.getEventService();
			 publisher = eventService.createPublisher(new URI(obean.getPublisherURI()), "scisoft.operation.STATUS_TOPIC");
		} catch (Exception e) {
			logger.error("Could not create publisher:",e);
		}

	}
	
	@Override
	public void worked(int amount) {
		count+=amount;
		double done = (double)count / (double)total;
		obean.setPercentComplete(done);
	    logger.info("Percent complete: "+(done*100));
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void subTask(String taskName) {
		obean.setMessage(taskName);
	    logger.info("Running: "+taskName);
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	public void setComplete() {
		if (publisher != null) {
			obean.setStatus(Status.COMPLETE);
			try {
				publisher.broadcast(obean);
			} catch (EventException e) {
				logger.error("Could not broadcast bean:",e);
			}
		}
	}

}
