package org.dawnsci.slice.server.test;

import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

import org.dawnsci.slice.client.DataClient;
import org.dawnsci.slice.server.Format;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
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
	
	@Test
	public void testDownsampledJPG() throws Exception {
		
		final DataClient client = new DataClient("http://localhost:8080/");
		client.setPath("c:/Work/results/TomographyDataSet.hdf5");
		client.setDataset("/entry/exchange/data");
		client.setSlice("[0,:1024,:1024]");
		client.setBin("MEAN:2x2");
		client.setFormat(Format.JPG);
		client.setHisto("MEAN");
		
		final BufferedImage image = client.getImage();
		if (image.getHeight()!=512) throw new Exception("Unexpected image height '"+image.getHeight()+"'");
		if (image.getWidth()!=512)  throw new Exception("Unexpected image height '"+image.getWidth()+"'");
	}

	
	@Test
	public void testDownsampledMJPG() throws Exception {
		
		final DataClient client = new DataClient("http://localhost:8080/");
		client.setPath("c:/Work/results/TomographyDataSet.hdf5");
		client.setDataset("/entry/exchange/data");
		client.setSlice("[700,:1024,:1024]");
		client.setBin("MEAN:2x2");
		client.setFormat(Format.MJPG);
		client.setHisto("MEAN");
		client.setSleep(100); // Default anyway is 100ms
		
		
		int i = 0;
		while(!client.isFinished()) {
			
			final BufferedImage image = client.take();
			if (image ==null) break; // Last image in stream is null.
			if (image.getHeight()!=512) throw new Exception("Unexpected image height '"+image.getHeight()+"'");
			if (image.getWidth()!=512)  throw new Exception("Unexpected image height '"+image.getWidth()+"'");
			++i;
			System.out.println("Image "+i+" found");
		}
	
		if (i != 20) throw new Exception("20 images were not found! "+i+" were!");
	}

	
	   static ImageData convertToSWT(BufferedImage bufferedImage) {
	        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
	            DirectColorModel colorModel = (DirectColorModel)bufferedImage.getColorModel();
	            PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
	            ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
	            for (int y = 0; y < data.height; y++) {
	                    for (int x = 0; x < data.width; x++) {
	                            int rgb = bufferedImage.getRGB(x, y);
	                            int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF)); 
	                            data.setPixel(x, y, pixel);
	                            if (colorModel.hasAlpha()) {
	                                    data.setAlpha(x, y, (rgb >> 24) & 0xFF);
	                            }
	                    }
	            }
	            return data;            
	        } else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
	            IndexColorModel colorModel = (IndexColorModel)bufferedImage.getColorModel();
	            int size = colorModel.getMapSize();
	            byte[] reds = new byte[size];
	            byte[] greens = new byte[size];
	            byte[] blues = new byte[size];
	            colorModel.getReds(reds);
	            colorModel.getGreens(greens);
	            colorModel.getBlues(blues);
	            RGB[] rgbs = new RGB[size];
	            for (int i = 0; i < rgbs.length; i++) {
	                    rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
	            }
	            PaletteData palette = new PaletteData(rgbs);
	            ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
	            data.transparentPixel = colorModel.getTransparentPixel();
	            WritableRaster raster = bufferedImage.getRaster();
	            int[] pixelArray = new int[1];
	            for (int y = 0; y < data.height; y++) {
	                    for (int x = 0; x < data.width; x++) {
	                            raster.getPixel(x, y, pixelArray);
	                            data.setPixel(x, y, pixelArray[0]);
	                    }
	            }
	            return data;
	        }
	        return null;
	    }

}
