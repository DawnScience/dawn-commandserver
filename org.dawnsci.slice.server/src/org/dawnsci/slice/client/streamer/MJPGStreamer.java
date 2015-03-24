package org.dawnsci.slice.client.streamer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

class MJPGStreamer extends AbstractStreamer<BufferedImage> {


	/**
	 * 
	 * @param url - URL to read from
	 * @param sleepTime - time to sleep between image reads, we don't want to use all CPU
	 * @param cacheSize - size of image cache. If image cache grows too large, they are DROPPED.
	 * @throws Exception
	 */
	public MJPGStreamer(URL url, long sleepTime, int cacheSize) throws Exception {
		init(url, sleepTime, cacheSize);
	}
	
	private static BufferedImage QUEUE_END = new BufferedImage(1, 1, 1);
	
	@Override
	protected BufferedImage getQueueEndObject() {
		return QUEUE_END;
	}

	@Override
	protected BufferedImage getFromStream(ByteArrayInputStream bais) throws Exception {
		return ImageIO.read(bais);
	}
}
