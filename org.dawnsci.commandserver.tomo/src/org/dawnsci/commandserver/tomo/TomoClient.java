package org.dawnsci.commandserver.tomo;

import java.net.URI;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.dawnsci.commandserver.tomo.beans.TomoBean;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.ISubmitter;

public class TomoClient {

	
	public static void main(String[] args) throws Exception {
		
		URI uri = new URI("tcp://sci-serv5.diamond.ac.uk:61616");
		
		TomoBean tbean = new TomoBean();
		tbean.setName("Test Jython");
		tbean.setMessage("A test jython execution");
		tbean.setRunDirectory("C:/tmp/");

		IEventService service = ActiveMQServiceHolder.getEventService();
		ISubmitter<TomoBean> submitter = service.createSubmitter(uri, "scisoft.tomo.SUBMISSION_QUEUE");

		submitter.submit(tbean);
	}
}
