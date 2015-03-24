package org.dawnsci.slice.client.streamer;

import java.net.URL;

import org.dawnsci.slice.server.Format;

public class StreamerFactory {

	public static IStreamer<?> getStreamer(URL url, long sleepTime, int cacheSize, Format format) throws Exception{
		
		if (format == Format.MJPG) {
			return new MJPGStreamer(url, sleepTime, cacheSize);
		} else if (format == Format.MDATA) {
			return new DataStreamer(url, sleepTime, cacheSize);
		}
		throw new Exception("No streamer for format "+format);
	}
}
