package org.dawnsci.commandserver.processing.process;

import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.monitor.IMonitor;
import org.eclipse.dawnsci.analysis.api.processing.IExecutionVisitor;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.OperationData;
import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;
import org.eclipse.dawnsci.analysis.dataset.slicer.Slicer;
import org.eclipse.scanning.api.event.core.IPublisher;

/**
 * Deals with sending percent complete from the pipeline
 * over the notification topic.
 * 
 * @author fcp94556
 *
 */
public class OperationVisitor implements IExecutionVisitor {

	private OperationBean obean;
	private IPublisher<OperationBean>  broadcaster;
	private int           total;
	private int           count;

	public OperationVisitor(ILazyDataset lz, OperationBean obean, IPublisher<OperationBean> broadcaster) throws Exception {
		this.obean       = obean;
		this.broadcaster = broadcaster;
		this.total       = Slicer.getSize(lz, obean.getSlicing());
	}


	@Override
	public void close() throws Exception {
		
	}


	@Override
	public void init(IOperation<? extends IOperationModel, ? extends OperationData>[] series,
			         ILazyDataset dataset) throws Exception {
		this.count = 0;
	
	}

	@Override
	public void notify(IOperation<? extends IOperationModel, ? extends OperationData> intermediateData,  OperationData data) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executed(OperationData result, IMonitor monitor) throws Exception {
		++count;
		double done = (double)count / (double)total;
		System.out.println(obean);
		obean.setPercentComplete(done);
		broadcaster.broadcast(obean);	
	}

}
