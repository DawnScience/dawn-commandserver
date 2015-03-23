package org.dawnsci.slice.client;

import java.net.URL;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DataStreamer extends AbstractStreamer<IDataset> {

	private static final Logger logger = LoggerFactory.getLogger(DataStreamer.class);
	
	/**
	 * 
	 * @param url - URL to read from
	 * @param sleepTime - time to sleep between image reads, we don't want to use all CPU
	 * @param cacheSize - size of image cache. If image cache grows too large, they are DROPPED.
	 * @throws Exception
	 */
	public DataStreamer(URL url, long sleepTime, int cacheSize) throws Exception {
		init(url, sleepTime, cacheSize);
	}
	
	public void run() {
		
       System.out.println("Data Streamer not supported!"); 
	}

	@Override
	protected boolean isCancelObject(IDataset bi) {
		if (bi.getSize()<1) return true;
		return false;
	}

}
