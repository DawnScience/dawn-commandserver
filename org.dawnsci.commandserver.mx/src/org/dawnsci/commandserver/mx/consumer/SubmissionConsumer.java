package org.dawnsci.commandserver.mx.consumer;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.mx.beans.DataCollectionsBean;
import org.dawnsci.commandserver.mx.dummy.DummyProcess;
import org.dawnsci.commandserver.mx.producer.RemoteSubmission;

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
		
		
		final String uri = args[0];
		
		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(uri);
		Connection    connection = connectionFactory.createConnection();
		Session   session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Queue queue = session.createQueue(args[1]);

		final MessageConsumer consumer = session.createConsumer(queue);
		connection.start();
		
		System.out.println("Starting consumer for submissions to queue "+args[1]);
        while (true) { // You have to kill it to stop it!
            
        	try {
        		
        		// Consumes messages from the queue.
	        	Message m = consumer.receive(1000);
	            if (m!=null) {
	            	TextMessage t = (TextMessage)m;
	            	ObjectMapper mapper = new ObjectMapper();
	            	
	            	final DataCollectionsBean bean = mapper.readValue(t.getText(), DataCollectionsBean.class);
	            	
                    if (bean.getStatusQueueName()!=null) { // We add this to the status list so that it can be rendered in the UI
                    	
                    	// Now we put the bean in the status queue and we 
                    	// start the process
                    	RemoteSubmission factory = new RemoteSubmission(uri);
                    	factory.setLifeTime(t.getJMSExpiration());
                    	factory.setPriority(t.getJMSPriority());
                    	factory.setTimestamp(t.getJMSTimestamp());
                    	factory.setMessageId(t.getJMSMessageID());
                    	
                    	factory.setQueueName(bean.getStatusQueueName());
                    	factory.submit(bean);
                    	
                    	final DummyProcess process = new DummyProcess(bean); // TODO Xia2 anyone?
                    	process.start();
                    	
                    	System.out.println("Started job "+bean.getName()+" messageid("+t.getJMSMessageID()+")");
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
