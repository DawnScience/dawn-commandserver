package org.dawnsci.commandserver.tomo.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.SubmissionConsumer;
import org.dawnsci.commandserver.tomo.beans.TomoBean;
import org.dawnsci.commandserver.tomo.process.TomoProcess;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * @author fcp94556
 *
 */
public class TomoSubmissionConsumer extends SubmissionConsumer {


	@Override
	public String getName() {
		return "Tomography Reconstruction Consumer";
	}

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return TomoBean.class;
	}


	@Override
	protected ProgressableProcess createProcess(URI uri, 
			                                    String statusTName,
			                                    String statusQName, 
			                                    StatusBean bean) throws Exception {

		return new TomoProcess(uri, statusTName, statusQName, (TomoBean)bean);
	}
	
	private static final long TWO_DAYS = 48*60*60*1000; // ms
	
	@Override
	protected long getMaximumRunningAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumTomoRunningAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumTomoRunningAge"));
		}
		return TWO_DAYS;
	}
	
	private static final long A_WEEK = 7*24*60*60*1000; // ms
	
	@Override
	protected long getMaximumCompleteAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumXia2CompleteAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumTomoCompleteAge"));
		}
		return A_WEEK;
	}


}
