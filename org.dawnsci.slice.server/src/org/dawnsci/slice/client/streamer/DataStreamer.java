package org.dawnsci.slice.client.streamer;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.URL;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.ShortDataset;

class DataStreamer extends AbstractStreamer<IDataset> {
	
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
	
	private static IDataset QUEUE_END = new ShortDataset();
	
	@Override
	protected IDataset getQueueEndObject() {
		return QUEUE_END;
	}

	@Override
	protected IDataset getFromStream(ByteArrayInputStream bais) throws Exception {
		ObjectInputStream oin = new ObjectInputStream(bais);
		try {
			return (IDataset)oin.readObject();
		} finally {
			oin.close();
		}
	}


}
