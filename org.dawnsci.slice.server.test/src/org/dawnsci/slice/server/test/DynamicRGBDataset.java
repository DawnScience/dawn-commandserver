package org.dawnsci.slice.server.test;

import java.awt.image.BufferedImage;

import org.dawnsci.slice.client.DataClient;
import org.dawnsci.slice.server.Format;
import org.eclipse.dawnsci.analysis.api.dataset.DataListenerDelegate;
import org.eclipse.dawnsci.analysis.api.dataset.IDataListener;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.IDynamicDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.RGBDataset;

public class DynamicRGBDataset extends RGBDataset implements IDynamicDataset {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2512465878034055747L;

	private DataListenerDelegate      delegate;
	private DataClient<BufferedImage> client;
	
	public DynamicRGBDataset(String uri, int... shape) {
		super(shape);
		delegate = new DataListenerDelegate();
		
    	client = new DataClient<BufferedImage>(uri);
    	client.setPath("RANDOM:"+shape[0]+"x"+shape[1]);
    	client.setFormat(Format.MJPG);
    	client.setHisto("MEAN");
    	client.setImageCache(10); // More than we will send...
    	client.setSleep(100);     // Default anyway is 100ms

    	while(!client.isFinished()) {
    		
    		//final BufferedImage image = client.take();
    		//if (image==null) break;
    		
    		//final IDataset set = plotService.createDataset(image);
    		
    	}
	}

	@Override
	public void addDataListener(IDataListener l) {
		delegate.addDataListener(l);
	}

	@Override
	public void removeDataListener(IDataListener l) {
		delegate.removeDataListener(l);
	}
	
}
