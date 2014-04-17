package org.dawnsci.commandserver.mx.consumer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.mx.beans.SubmissionBean;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This consumer monitors a queue and starts runs based
 * on what is submitted.
 * 
 * @author fcp94556
 *
 */
public class SubmissionConsumer {

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		if (!checkArguments(args)) return;
		
		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(args[0]);
		Connection    connection = connectionFactory.createConnection();
		Session   session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = session.createQueue(args[1]);

		final MessageConsumer consumer = session.createConsumer(queue);
		connection.start();
		
		System.out.println("Starting consumer for submissions to queue "+args[1]);
        while (true) { // You have to kill it to stop it!
            
        	try {
	        	Message m = consumer.receive(1000);
	            if (m!=null) {
	            	TextMessage t = (TextMessage)m;
	            	try {
	            		ObjectMapper mapper = new ObjectMapper();
	            		final SubmissionBean bean = mapper.readValue(t.getText(), SubmissionBean.class);
	            		System.out.println("Submission found: "+bean.getName());
	
	            	} catch (Exception ne) {
	            		System.out.println(m+" is not a submission.");
	            	}
	            }
        	} catch (Throwable ne) {
        		// Really basic error reporting, they have to pipe to file.
        		ne.printStackTrace();
        	}
		}

	}

	private static String USAGE = "Usage: java -jar <...> "+SubmissionConsumer.class.getName()+" <URI ACTIVEMQ> <MX SUBMISSION QUEUE NAME>\n"+
	                              "Example: java -jar ispyb.jar "+SubmissionConsumer.class.getName()+" tcp://ws097.diamond.ac.uk:61616 scisoft.xia2.SUBMISSION_QUEUE";
	private static boolean checkArguments(String[] args) {
		
        if (args == null || args.length!=2) {
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
        
        return true;

	}
}
