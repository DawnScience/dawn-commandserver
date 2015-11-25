package org.dawnsci.commandserver.processing.process;

import java.io.File;
import java.util.Arrays;

import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.dataset.Slice;
import org.eclipse.dawnsci.analysis.api.dataset.SliceND;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.io.ILoaderService;
import org.eclipse.dawnsci.analysis.api.metadata.AxesMetadata;
import org.eclipse.dawnsci.analysis.api.monitor.IMonitor;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.IExecutionVisitor;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.dawnsci.analysis.api.processing.IOperationService;
import org.eclipse.dawnsci.analysis.dataset.slicer.SliceFromSeriesMetadata;
import org.eclipse.dawnsci.analysis.dataset.slicer.Slicer;
import org.eclipse.dawnsci.analysis.dataset.slicer.SourceInformation;
import org.eclipse.dawnsci.hdf5.operation.HierarchicalFileExecutionVisitor;

public class OperationExecution {

	private static IOperationService   oservice;
	private static IPersistenceService pservice;
	private static ILoaderService      lservice;
	
	// Set by OSGI
	public static void setOperationService(IOperationService s) {
		oservice = s;
	}
	// Set by OSGI
	public static void setPersistenceService(IPersistenceService s) {
		pservice = s;
	}
	// Set by OSGI
	public static void setLoaderService(ILoaderService s) {
		lservice = s;
	}

	private IOperationContext context;
	

	/**
	 * Can be used to execute an operation process.
	 * @param obean
	 * @throws Exception
	 */
	public void run(final OperationBean obean) throws Exception {
		
		IPersistentFile file = pservice.createPersistentFile(obean.getPersistencePath());
		try {
			// We should get these back exactly as they were defined.
		    final IOperation[] ops = file.getOperations();
		    
		    // Create a context and run the pipeline
		    this.context = oservice.createContext();
		    context.setSeries(ops);
		    context.setExecutionType(obean.getExecutionType());
		    context.setParallelTimeout(obean.getParallelTimeout());
		    
		    final IDataHolder holder = lservice.getData(obean.getFilePath(), new IMonitor.Stub());
		    //take a local view
		    final ILazyDataset lz    = holder.getLazyDataset(obean.getDatasetPath()).getSliceView();
		    //TODO need to set up Axes and SliceSeries metadata here
		   
		    SourceInformation si = new SourceInformation(obean.getFilePath(), obean.getDatasetPath(), lz);
		    lz.setMetadata(new SliceFromSeriesMetadata(si));
		    AxesMetadata axm = lservice.getAxesMetadata(lz, obean.getFilePath(), obean.getAxesNames());
			lz.setMetadata(axm);
		    
		    context.setData(lz);
		    context.setSlicing(obean.getSlicing());
		    
		    //Create visitor to save data
		    final IExecutionVisitor visitor = new HierarchicalFileExecutionVisitor(obean.getOutputFilePath());
		    context.setVisitor(visitor);
		    
		    // We create a monitor which publishes information about what
		    // operation was completed.
		    int[] shape = lz.getShape();
		    int work = getTotalWork(context.getSlicing().convertToSlice(), shape,context.getDataDimensions());
		    context.setMonitor(new OperationMonitor(obean, work));
		    
		    oservice.execute(context);
		    
		} finally {
			file.close();
			
			if (obean.isDeletePersistenceFile()) {
			    final File persFile = new File(obean.getPersistencePath());
			    persFile.delete();
			}
		}

	}

	
	public void stop()  {
		
		if (context!=null && context.getMonitor()!=null && context.getMonitor() instanceof OperationMonitor) {
			OperationMonitor mon = (OperationMonitor)context.getMonitor();
			mon.setCancelled(true);
		}
	}

	
	private int getTotalWork(Slice[] slices, int[] shape, int[] datadims) {
		SliceND slice = new SliceND(shape, slices);
		int[] nShape = slice.getShape();

		int[] dd = datadims.clone();
		Arrays.sort(dd);
		
		 int n = 1;
		 for (int i = 0; i < nShape.length; i++) {
			 if (Arrays.binarySearch(dd, i) < 0) n *= nShape[i];
		 }
		return n;
	}

}
