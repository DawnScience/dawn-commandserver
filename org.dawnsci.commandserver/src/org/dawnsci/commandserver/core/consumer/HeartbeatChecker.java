package org.dawnsci.commandserver.core.consumer;

import java.net.URI;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.HeartbeatEvent;
import org.eclipse.scanning.api.event.alive.IHeartbeatListener;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks for the heartbeat of a named consumer.
 * 
 * @author Matthew Gerring
 *
 */
public class HeartbeatChecker {
	
	private static final Logger logger = LoggerFactory.getLogger(HeartbeatChecker.class);

	private URI    uri;
	private String consumerName;
	private long   listenTime;
	private volatile boolean ok = false;
	
	public HeartbeatChecker(URI uri, String consumerName, long listenTime) {
		this.uri          = uri;
		this.consumerName = consumerName;
		this.listenTime   = listenTime;
	}
	
	public void checkPulse() throws Exception {
		
		final IEventService service = ActiveMQServiceHolder.getEventService();
		ISubscriber<IHeartbeatListener>	subscriber = service.createSubscriber(uri, IEventService.HEARTBEAT_TOPIC);
        ok = false;
        
        try {
        	subscriber.addListener(new IHeartbeatListener() {
        		@Override
        		public Class<HeartbeatBean> getBeanClass() {
        			return HeartbeatBean.class;
        		}

        		@Override
        		public void heartbeatPerformed(HeartbeatEvent evt) {
        			HeartbeatBean bean = evt.getBean();
        			if (!consumerName.equals(bean.getConsumerName())) {
        				return;
        			}
        			logger.trace(bean.getConsumerName()+ " is alive and well.");
        			ok = true;

        		}
        	});

            Thread.sleep(listenTime);
            
            if (!ok) throw new Exception(consumerName+" Consumer heartbeat absent.\nIt is either stopped or unresponsive.\nPlease contact your support representative.");
        	

        } finally {
        	subscriber.disconnect();
        }
	}

}
