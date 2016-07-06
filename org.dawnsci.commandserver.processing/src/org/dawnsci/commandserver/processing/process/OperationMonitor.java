package org.dawnsci.commandserver.processing.process;

import org.eclipse.january.IMonitor;
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
	
	public OperationMonitor(OperationBean obean, int total) throws Exception {
		this.obean       = obean;
		this.total       = total;
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

}
