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


	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		if (!checkArguments(args)) return;
		
		
		final String uri         = args[0];
		final String submitQName = args[1];
		final String statusTName = args[2];
		final String statusQName = args[3];
		
        final SubmissionConsumer instance = new MXSubmissionConsumer(uri, submitQName, statusTName, statusQName);
        instance.start();
	}
	


	public MXSubmissionConsumer(String uri, 
			                    String submitQName,
			                    String statusTName, 
			                    String statusQName) {
		
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
			                                    StatusBean bean) {

		return new Xia2Process(uri, statusTName, statusQName, bean);
	}


	private static String USAGE = "Usage: java -jar <...> "+SubmissionConsumer.class.getName()+" <URI ACTIVEMQ> <MX SUBMISSION QUEUE NAME> <MX TOPIC NAME> <STATUS QUEUE NAME\n"+
	                              "Example: java -jar ispyb.jar "+SubmissionConsumer.class.getName()+" tcp://ws097.diamond.ac.uk:61616 scisoft.xia2.SUBMISSION_QUEUE scisoft.xia2.STATUS_TOPIC scisoft.xia2.STATUS_QUEUE";
	protected static boolean checkArguments(String[] args) {
		
        if (args == null || args.length!=4) {
        	System.out.println(USAGE);
        	return false;
        }
        
        if (!args[0].startsWith("tcp://")) {
        	System.out.println(USAGE);
        	return false;
        }
        
        if ("".equals(args[1])) {
        	System.out.println(USAGE);
        	return false;
        }
        
        if ("".equals(args[2])) {
        	System.out.println(USAGE);
        	return false;
        }
        
        if ("".equals(args[3])) {
        	System.out.println(USAGE);
        	return false;
        }
        
        return true;

	}

}
