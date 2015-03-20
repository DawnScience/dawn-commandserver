package org.dawnsci.slice.server.test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;

import org.dawb.common.services.IPlotImageService;
import org.dawb.common.services.ServiceManager;
import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.workbench.ui.editors.ImageEditor;
import org.dawnsci.slice.client.DataClient;
import org.dawnsci.slice.server.Format;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace.DownsampleType;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Runs as Junit Plugin test because runs up user interface with stream. 
 * Start the Data Server before running this test!
 * 
 */
public class ClientPluginTest {

	/**
	 * Test opens stream in plotting system.
	 * @throws Exception
	 */
	@Test
	public void testPlottingSystemStream() throws Exception {
		
		final Bundle bun  = Platform.getBundle("org.dawb.workbench.ui.test");
		String path = (bun.getLocation()+"/src/org/dawb/workbench/ui/editors/test/tln_1_0001.cbf");
		path = path.substring("reference:file:".length());

		final IWorkbenchPage     page = EclipseUtils.getPage();		
		final IFileStore externalFile = EFS.getLocalFileSystem().fromLocalFile(new File(path));
 		final IEditorPart        part = page.openEditor(new FileStoreEditorInput(externalFile), ImageEditor.ID);
		
 		page.activate(part);
 		page.setPartState(page.getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);
 
		final IPlottingSystem sys = (IPlottingSystem)part.getAdapter(IPlottingSystem.class);

   		EclipseUtils.delay(2000); // While image loads.
		final Collection<ITrace>   traces= sys.getTraces(IImageTrace.class);
		final IImageTrace          imt = (IImageTrace)traces.iterator().next();
    	imt.setDownsampleType(DownsampleType.POINT); // Fast!
    	imt.setRescaleHistogram(false); // Fast!
    	
    	IPlotImageService plotService = (IPlotImageService)ServiceManager.getService(IPlotImageService.class);
     		
    	final DataClient client = new DataClient("http://localhost:8080/");
    	client.setPath("c:/Work/results/TomographyDataSet.hdf5");
    	client.setDataset("/entry/exchange/data");
    	client.setSlice("[700,:1024,:1024]");
    	client.setBin("MEAN:2x2");
    	client.setFormat(Format.MJPG);
    	client.setHisto("MEAN");
    	client.setSleep(100); // Default anyway is 100ms


    	while(!client.isFinished()) {

    		final BufferedImage image = client.take();
    		if (image==null) break;
    		
    		IDataset set = plotService.createDataset(image);
    		imt.setData(set, null, false);
    		EclipseUtils.delay(1000);
    	}

 		
	}
	
}
