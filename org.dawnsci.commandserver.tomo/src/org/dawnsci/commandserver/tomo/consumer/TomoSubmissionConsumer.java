package org.dawnsci.commandserver.tomo.consumer;

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

	private static String USAGE = "Usage: java -jar <...> "+SubmissionConsumer.class.getName()+" <URI ACTIVEMQ> <MX SUBMISSION QUEUE NAME> <MX TOPIC NAME> <STATUS QUEUE NAME\n"+
            "Example: java -jar ispyb.jar "+TomoSubmissionConsumer.class.getName()+" tcp://ws097.diamond.ac.uk:61616 scisoft.tomo.SUBMISSION_QUEUE scisoft.tomo.STATUS_TOPIC scisoft.tomo.STATUS_QUEUE";

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		if (!checkArguments(args, USAGE)) return;
		
		
		final String uri         = args[0];
		final String submitQName = args[1];
		final String statusTName = args[2];
		final String statusQName = args[3];
		
        final SubmissionConsumer instance = new TomoSubmissionConsumer(uri, submitQName, statusTName, statusQName);
        instance.start();
	}
	

	@Override
	public String getName() {
		return "Tomography Reconstruction Consumer";
	}


	public TomoSubmissionConsumer(String uri, 
			                    String submitQName,
			                    String statusTName, 
			                    String statusQName) throws Exception {
		
		super(uri, submitQName, statusTName, statusQName);
	}

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return TomoBean.class;
	}


	@Override
	protected ProgressableProcess createProcess(String uri, 
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
