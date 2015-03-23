package org.dawnsci.slice.client;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

abstract class AbstractStreamer<T> implements IStreamer<T>, Runnable {

	
	protected BlockingQueue<T> queue;
	protected InputStream      in;
	protected long             sleepTime;
	protected long             droppedImages = 0;
	protected boolean          isFinished;

	protected URLConnection init(URL url, long sleepTime, int cacheSize) throws Exception {

		URLConnection  conn = url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);

 		this.queue      = new LinkedBlockingQueue<T>(cacheSize); // TODO How many images can be in the queue?
		this.in         = new BufferedInputStream(conn.getInputStream());
		this.sleepTime  = sleepTime;

		return conn;
	}
	
	/**
	 * Blocks until image added. Once null is added, we are done.
	 * @return Image or null when finished.
	 * 
	 * @throws InterruptedException
	 */
	public T take() throws InterruptedException {
		T bi = queue.take(); // Might get interrupted
		if (isCancelObject(bi)) {
			setFinished(true);
			return null;
		}
		return bi;
	}

	protected abstract boolean isCancelObject(T bi);

	public long getDroppedImages() {
		return droppedImages;
	}

	public void start() {
		Thread thread = new Thread(this);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.setDaemon(true);
		thread.setName("MJPG Streamer");
		thread.start();
	}

	/**
	 * Call to tell the streamer to stop adding images to its queue.
	 * @param b
	 */
	public void setFinished(boolean b) {
		this.isFinished = b;
	}


}
