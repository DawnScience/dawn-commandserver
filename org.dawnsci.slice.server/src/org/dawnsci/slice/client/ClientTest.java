package org.dawnsci.slice.client;

import java.util.Arrays;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.junit.Test;

/**
 * Test tests require that the DataServer is going and that the
 * data is at the pre-supposed locations.
 * 
 * TODO make this into a replicable unit test.
 * 
 * @author fcp94556
 *
 */
public class ClientTest {

	@Test
	public void testFullData() throws Exception {
		
		final DataClient client = new DataClient("http://localhost:8080/");
		client.setPath("c:/Work/results/TomographyDataSet.hdf5");
		client.setDataset("/entry/exchange/data");
		client.setSlice("[0,:1024,:1024]");
		
		final IDataset data = client.getData();
		if (!Arrays.equals(data.getShape(), new int[]{1024, 1024})) {
			throw new Exception("Unexpected shape "+Arrays.toString(data.getShape()));
		}

	}
	
	@Test
	public void testDownsampledData() throws Exception {
		
		final DataClient client = new DataClient("http://localhost:8080/");
		client.setPath("c:/Work/results/TomographyDataSet.hdf5");
		client.setDataset("/entry/exchange/data");
		client.setSlice("[0,:1024,:1024]");
		client.setBin("MEAN:2x2");
		
		final IDataset data = client.getData();
		if (!Arrays.equals(data.getShape(), new int[]{512, 512})) {
			throw new Exception("Unexpected shape "+Arrays.toString(data.getShape()));
		}

	}
}
