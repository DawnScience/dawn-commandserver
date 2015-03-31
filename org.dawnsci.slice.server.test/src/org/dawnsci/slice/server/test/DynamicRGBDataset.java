package org.dawnsci.slice.server.test;

import java.awt.image.BufferedImage;
import java.io.Serializable;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawnsci.slice.client.DataClient;
import org.dawnsci.slice.server.Format;
import org.eclipse.dawnsci.analysis.api.dataset.DataEvent;
import org.eclipse.dawnsci.analysis.api.dataset.DataListenerDelegate;
import org.eclipse.dawnsci.analysis.api.dataset.IDataListener;
import org.eclipse.dawnsci.analysis.api.dataset.IDynamicDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.RGBDataset;
import org.eclipse.dawnsci.plotting.api.image.IPlotImageService;

public class DynamicRGBDataset extends RGBDataset implements IDynamicDataset {
	
	private static IPlotImageService plotService;
	public static void setPlotImageService(IPlotImageService service) {
		plotService = service;
	}
	public DynamicRGBDataset() {
		// OSGi
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2512465878034055747L;

	private DataListenerDelegate      delegate;
	private DataClient<BufferedImage> client;
	
	public DynamicRGBDataset(DataClient<BufferedImage> client, int... shape) {
		super(shape);
		this.delegate = new DataListenerDelegate();
		this.client = client;
	}
	
	/**
	 * Starts notifying the IDataListener's with the current 
	 * thread, blocking until there are no more images.
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		start(-1);
	}
	/**
	 * Starts notifying the IDataListener's with the current 
	 * thread, blocking until there are no more images or if
	 * maxImages>0 until that number of images has been received.
	 * 
	 * @throws Exception
	 */
	public void start(int maxImages) throws Exception {
    	
		int count = 0;
		while(!client.isFinished()) {
    		
    		final BufferedImage image = client.take();
    		if (image==null) break;
    		
    		final RGBDataset set = (RGBDataset)plotService.createDataset(image);
    		setBuffer(set.getBuffer());
    		setData();
    		setShape(set.getShape());
    		delegate.fire(new DataEvent(set));
    		
    		++count;
    		if (count>maxImages) return;
    		
    		EclipseUtils.delay(client.getSleep());
    	}

	}

	private void setBuffer(Serializable buffer) {
		odata = buffer;
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
