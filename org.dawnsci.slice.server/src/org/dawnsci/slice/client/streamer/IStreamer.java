package org.dawnsci.slice.client.streamer;

/**
 * Interface used to stream either JPG or IDataset from
 * server stream.
 * 
 * @author fcp94556
 *
 * @param <T>
 */
public interface IStreamer<T> {

	/**
	 * Start the streamer
	 */
	void start();

	/**
	 * Gets the image
	 * @return
	 */
	T take()  throws InterruptedException ;

	/**
	 * Gets the count of dropped images from this stream
	 * @return
	 */
	long getDroppedImages();

	/**
	 * Call to stop streamer from streaming.
	 * @param b
	 */
	void setFinished(boolean b);

}
