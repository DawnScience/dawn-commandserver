package org.dawnsci.slice.server.test;

import java.awt.image.BufferedImage;
import java.util.Collection;

import org.dawb.common.services.ServiceManager;
import org.dawb.common.ui.util.EclipseUtils;
import org.dawnsci.slice.client.DataClient;
import org.dawnsci.slice.server.Format;
import org.eclipse.dawnsci.analysis.api.dataset.DataEvent;
import org.eclipse.dawnsci.analysis.api.dataset.IDataListener;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.dataset.impl.Random;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.image.IPlotImageService;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace.DownsampleType;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.junit.Test;

/**
 * Runs as Junit Plugin test because runs up user interface with stream. 
 * Start the Data Server before running this test!
 * 
 */
public class ClientPluginTest {

	private volatile int count = 0;
	
	/**
	 * Test opens stream in plotting system.
	 * @throws Exception
	 */
	@Test
	public void testDynamicDataset() throws Exception {
		
		DataClient<BufferedImage> client = new DataClient<BufferedImage>("http://localhost:8080/");
    	client.setPath("RANDOM:512x512");
    	client.setFormat(Format.MJPG);
    	client.setHisto("MEAN");
    	client.setImageCache(10); // More than we will send...
    	client.setSleep(100);     // Default anyway is 100ms

    	IWorkbenchPart part = openView();
		 
		final IPlottingSystem   sys = (IPlottingSystem)part.getAdapter(IPlottingSystem.class);
		final DynamicRGBDataset rgb = new DynamicRGBDataset(client, 512, 512);
		sys.createPlot2D(rgb, null, null);

		rgb.start(100); // blocks until 100 images received.
	}
	
	/**
	 * Test opens stream in plotting system.
	 * @throws Exception
	 */
	@Test
	public void testHDF5Stream() throws Exception {
		
		IWorkbenchPart part = openView();
 
		final IPlottingSystem sys = (IPlottingSystem)part.getAdapter(IPlottingSystem.class);
		sys.createPlot2D(Random.rand(new int[]{1024, 1024}), null, null);
		
   		final Collection<ITrace>   traces= sys.getTraces(IImageTrace.class);
		final IImageTrace          imt = (IImageTrace)traces.iterator().next();
    	imt.setDownsampleType(DownsampleType.POINT); // Fast!
    	imt.setRescaleHistogram(false); // Fast!
    	
    	IPlotImageService plotService = (IPlotImageService)ServiceManager.getService(IPlotImageService.class);
     		
    	final DataClient<BufferedImage> client = new DataClient<BufferedImage>("http://localhost:8080/");
    	client.setPath("c:/Work/results/TomographyDataSet.hdf5");
    	client.setDataset("/entry/exchange/data");
    	client.setSlice("[700,:1024,:1024]");
    	client.setBin("MEAN:2x2");
    	client.setFormat(Format.MJPG);
    	client.setHisto("MEDIAN");
    	client.setImageCache(25); // More than we will send...
    	client.setSleep(100); // Default anyway is 100ms


    	try {
    		
    		int i = 0;
	    	while(!client.isFinished()) {
	
	    		final BufferedImage image = client.take();
	    		if (image==null) break;
	    		
	    		final IDataset set = plotService.createDataset(image);
	    		
	    		Display.getDefault().syncExec(new Runnable() {
	    			public void run() {
	    	    		imt.setData(set, null, false);
	    	    		sys.repaint();
	    			}
	    		});
	    		System.out.println("Slice "+i+" plotted");
	    		++i;
	    		EclipseUtils.delay(100);
	    	}
    	} catch (Exception ne) {
    		client.setFinished(true);
    		throw ne;
    	}

 		
	}
	
	
	/**
	 * Test opens stream in plotting system.
	 * @throws Exception
	 */
	@Test
	public void testStreamSpeed() throws Exception {
		
		IWorkbenchPart part = openView();
 
		final IPlottingSystem sys = (IPlottingSystem)part.getAdapter(IPlottingSystem.class);
		sys.createPlot2D(Random.rand(new int[]{1024, 1024}), null, null);
		
   		final Collection<ITrace>   traces= sys.getTraces(IImageTrace.class);
		final IImageTrace          imt = (IImageTrace)traces.iterator().next();
    	imt.setDownsampleType(DownsampleType.POINT); // Fast!
    	imt.setRescaleHistogram(false); // Fast!
    	
    	IPlotImageService plotService = (IPlotImageService)ServiceManager.getService(IPlotImageService.class);
     		
    	final DataClient<BufferedImage> client = new DataClient<BufferedImage>("http://localhost:8080/");
    	client.setPath("RANDOM:1024x1024");
    	client.setFormat(Format.MJPG);
    	client.setHisto("MEAN");
    	client.setImageCache(10); // More than we will send...
    	client.setSleep(15); // Default anyway is 100ms


    	try {
    		
    		int i = 0;
	    	while(!client.isFinished()) {
	
	    		final BufferedImage image = client.take();
	    		if (image==null) break;
	    		
	    		final IDataset set = plotService.createDataset(image);
	    		
	    		Display.getDefault().syncExec(new Runnable() {
	    			public void run() {
	    	    		imt.setData(set, null, false);
	    	    		sys.repaint();
	    			}
	    		});
	    		System.out.println("Slice "+i+" plotted");
	    		++i;
				if (i>100) {
					client.setFinished(true);
					break; // That's enough of that
				}
	    		EclipseUtils.delay(15);

	    	}
	    	
			System.out.println("Received images = "+i);
			System.out.println("Dropped images = "+client.getDroppedImageCount());

    	} catch (Exception ne) {
    		client.setFinished(true);
    		throw ne;
    	}

 		
	}


	private IWorkbenchPart openView() throws PartInitException {
		final IWorkbenchPage     page = EclipseUtils.getPage();		
		IViewPart part = page.showView("org.dawnsci.processing.ui.view.vanillaPlottingSystemView", null, IWorkbenchPage.VIEW_ACTIVATE);		
 		page.activate(part);
 		page.setPartState(page.getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);
 		return part;
	}

}
