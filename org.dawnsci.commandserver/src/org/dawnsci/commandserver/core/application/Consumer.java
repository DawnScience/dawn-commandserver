package org.dawnsci.commandserver.core.application;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;

/**
 * 
 * This application is used to start and stop any analysis consumer.
 * 
 * Arguments:
 * 
 * -uri       activemq URI, e.g. tcp://sci-serv5.diamond.ac.uk:61616 
 * -submit    queue to submit e.g. scisoft.xia2.SUBMISSION_QUEUE 
 * -topic     topic to notify e.g. scisoft.xia2.STATUS_TOPIC 
 * -status    queue for status e.g. scisoft.xia2.STATUS_QUEUE 
 * -bundle    bundle for consumer e.g. org.dawnsci.commandserver.mx 
 * -consumer  consumer class e.g. org.dawnsci.commandserver.mx.consumer.MXSubmissionConsumer
 * 
 * 
 * @author fcp94556
 *
 */
public class Consumer implements IApplication {

	private IConsumerExtension consumer;

	@Override
	public Object start(IApplicationContext context) throws Exception {
	
		final Map      args          = context.getArguments();
		final String[] configuration = (String[])args.get("application.args");
        
		Map<String, String> conf = new HashMap<String, String>(7);
		for (int i = 0; i < configuration.length; i++) {
			final String pkey = configuration[i];
			if (pkey.startsWith("-")) {
				conf.put(pkey.substring(1), configuration[i+1]);
			}
		}
		
		final Bundle bundle = Platform.getBundle(conf.get("bundle"));
		
		@SuppressWarnings("unchecked")
		final Class<? extends IConsumerExtension> clazz = (Class<? extends IConsumerExtension>) bundle.loadClass(conf.get("consumer"));
		
		this.consumer = clazz.newInstance();
		consumer.init(conf);
		consumer.start(); // blocking method
		
		return consumer;
	}

	@Override
	public void stop() {
		try {
			consumer.stop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
