package org.dawnsci.slice.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.BlockingQueue;

import javax.imageio.ImageIO;

import org.dawnsci.slice.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MJPGStreamer extends AbstractStreamer<BufferedImage> {

	private static final Logger logger = LoggerFactory.getLogger(MJPGStreamer.class);
	
	private String                       delimiter;

	/**
	 * 
	 * @param url - URL to read from
	 * @param sleepTime - time to sleep between image reads, we don't want to use all CPU
	 * @param cacheSize - size of image cache. If image cache grows too large, they are DROPPED.
	 * @throws Exception
	 */
	public MJPGStreamer(URL url, long sleepTime, int cacheSize) throws Exception {
		
		URLConnection conn = init(url, sleepTime, cacheSize);
		
        String contentType = conn.getContentType();
        if (!contentType.startsWith(Constants.MCONTENT_TYPE)) throw new Exception("getImages() may only be used with "+Constants.MCONTENT_TYPE);

        this.delimiter  = contentType.split("\\;boundary=")[1];
	}
	
	public void run() {
		
		isFinished = false;
		try {
			final StringBuilder buf = new StringBuilder();

			int c       = -1;
			boolean foundImage = false;
			
			while(!isFinished && (c=in.read())> -1 ) {
				
				buf.append((char)c);
				if (buf.length()>0 && buf.charAt(buf.length()-1)  == '\n') { // Line found
					
					final String line = buf.toString().trim();
					if (line.equals("--"+delimiter)) { // We found a new image
						foundImage = true;
					}
					if (foundImage && line.startsWith("Content-Length: ")) {
						int clength         = Integer.parseInt(line.split("\\:")[1].trim());
						BufferedImage image = readImage(in, clength);
						if (image == null || isFinished) return;
						
                        if (queue.remainingCapacity()<1) {
                        	Object gone = queue.poll(); // Goodbye
                        	if (gone!=null) {
                        		droppedImages+=1;
                               	logger.trace("We dropped an image of size "+clength+" bytes when reading an MJPG Stream");
                        	}
                        }
						queue.add(image);
						foundImage = false;
						
						Thread.sleep(sleepTime); // We don't want to use all the CPU!
					}
					
					buf.delete(0, buf.length());
					continue;
				}
			}
			
		} catch (Exception ne) {
			setFinished(true);
			logger.error("Cannot read input stream in "+getClass().getSimpleName(), ne);
			
		} finally {
			try {
				in.close();
			} catch (Exception ne) {
				logger.error("Cannot close connection!", ne);
			}
			// Cannot have null, instead add tiny empty image
			queue.add(new BufferedImage(1, 1, 1));
		}
	}
	
	
	
	private BufferedImage readImage(InputStream in, int clength) throws Exception {
		
		int c= -1;
		// Scoot down until no more new lines (this looses first character of JPG)
		while((c=in.read())> -1) {
			if (c=='\r') continue;
			if (c=='\n') continue;
			break;
		}
			
		byte[] imageBytes = new byte[clength + 1];

		imageBytes[0] = (byte)255;
		int offset    = 1;
		int numRead   = 0;
		while (!isFinished && offset < imageBytes.length && (numRead=in.read(imageBytes, offset, imageBytes.length-offset)) >= 0) {
			offset += numRead;
		}       

		if (isFinished) return null;
		
		ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);			
		return ImageIO.read(bais);

	}

	@Override
	protected boolean isCancelObject(BufferedImage bi) {
		if (bi.getWidth()<2 && bi.getHeight()<2) {
			return true;
		}
		return false;
	}
}
