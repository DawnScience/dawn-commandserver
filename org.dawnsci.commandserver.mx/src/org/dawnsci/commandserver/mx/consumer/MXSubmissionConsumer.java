package org.dawnsci.commandserver.mx.consumer;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.SubmissionConsumer;
import org.dawnsci.commandserver.mx.beans.ProjectBean;
import org.dawnsci.commandserver.mx.process.Xia2Process;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * @author fcp94556
 *
 */
public class MXSubmissionConsumer extends SubmissionConsumer {

	private static String USAGE = "Usage: java -jar <...> "+SubmissionConsumer.class.getName()+" <URI ACTIVEMQ> <MX SUBMISSION QUEUE NAME> <MX TOPIC NAME> <STATUS QUEUE NAME\n"+
            "Example: java -jar ispyb.jar "+MXSubmissionConsumer.class.getName()+" tcp://ws097.diamond.ac.uk:61616 scisoft.xia2.SUBMISSION_QUEUE scisoft.xia2.STATUS_TOPIC scisoft.xia2.STATUS_QUEUE";

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
		
        final SubmissionConsumer instance = new MXSubmissionConsumer(uri, submitQName, statusTName, statusQName);
        instance.start();
	}
	

	@Override
	public String getName() {
		return "Multi-crystal Reprocessing Consumer";
	}


	public MXSubmissionConsumer(String uri, 
			                    String submitQName,
			                    String statusTName, 
			                    String statusQName) throws Exception {
		
		super(uri, submitQName, statusTName, statusQName);
	}

	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		return ProjectBean.class;
	}


	@Override
	protected ProgressableProcess createProcess(String uri, 
			                                    String statusTName,
			                                    String statusQName, 
			                                    StatusBean bean) throws Exception {

		return new Xia2Process(uri, statusTName, statusQName, (ProjectBean)bean);
	}

	
	private static final long TWO_DAYS = 48*60*60*1000; // ms
	
	@Override
	protected long getMaximumRunningAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumXia2RunningAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumXia2RunningAge"));
		}
		return TWO_DAYS;
	}
	
	private static final long A_WEEK = 7*24*60*60*1000; // ms
	
	@Override
	protected long getMaximumCompleteAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumXia2CompleteAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumXia2CompleteAge"));
		}
		return A_WEEK;
	}


}
