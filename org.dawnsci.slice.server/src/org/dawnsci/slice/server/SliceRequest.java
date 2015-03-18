package org.dawnsci.slice.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.dataset.ILazyDataset;
import org.eclipse.dawnsci.analysis.api.dataset.Slice;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.monitor.IMonitor;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.plotting.api.histogram.HistogramBound;
import org.eclipse.dawnsci.plotting.api.histogram.IImageService;
import org.eclipse.dawnsci.plotting.api.histogram.ImageServiceBean;
import org.eclipse.dawnsci.plotting.api.histogram.ImageServiceBean.HistoType;
import org.eclipse.dawnsci.plotting.api.histogram.ImageServiceBean.ImageOrigin;
import org.eclipse.jetty.server.Request;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/**
 * There are one of these objects per session.
 * 
 * So a user only blocks their own session if they
 * do an unfriendly slice.
 * 
 * Parameters which may be set in the request:
 *    Essential
 *    =========
 *    path`   - path to file or directory for loader factory to use.
 *    
 *    Optional
 *    ========
 *    dataset - dataset name, by default the dataset at position 0 will be returned
 *    
 *    slice`  - Provides the slice in the form of that required org.eclipse.dawnsci.analysis.api.dataset.Slice.convertFromString(...)
 *              for example: [0,:1024,:1024]. If left unset and data not too large, will send while dataset, no slice.
 *              
 *    bin     - downsample  As in Downsample.encode(...) / Downsample.decode(...) ; examples: 'MEAN:2x3', 'MAXIMUM:2x2' 
 *              by default no downsampling is done
 *              
 *    format  - One of Format.values():
 *              DATA - zipped slice, binary (default)
 *              JPG  - JPG made using IImageService to make the image
 *              PNG  - PNG made using IImageService to make the image
 *              
 *    histo   - Encoding of histo to the rules of ImageServiceBean.encode(...) / ImageServiceBean.decode(...)
 *              Example: "MEAN", "OUTLIER_VALUES:5-95"
 *              Only used when an actual image is requested.
 * 
 *    `URL encoded.
 *    
 *    
 *     * Example in GET format (POST is also ok):
 * 
 *      http://localhost:8080/?path=c%3A/Work/results/TomographyDataSet.hdf5&dataset=/entry/exchange/data&slice=[0,%3A1024,%3A1024]
 *      
 *      Or in a browser:
 *      http://localhost:8080/?path=c%3A/Work/results/TomographyDataSet.hdf5&dataset=/entry/exchange/data&slice=[0,%3A1024,%3A1024]&bin=MAXIMUM:2x2&format=JPG
 *
 * @author fcp94556
 *
 */
class SliceRequest implements HttpSessionBindingListener {	
	
	private ReentrantLock lock;
	private String sessionId;


	// Actually the SliceRequest
	SliceRequest(String sessionId) {
    	this.lock = new ReentrantLock();
    	this.sessionId = sessionId;
	}
	
	public void slice(String              target,
					  Request             baseRequest,
					  HttpServletRequest  request,
					  HttpServletResponse response) throws Exception {
		try {
		    lock.lock(); // Blocks so that one thread at a time does the slice for a given session.
		    doSlice(target, baseRequest, request, response);
		} finally {
			lock.unlock();
		}
	}

	protected void doSlice(String              target,
						   Request             baseRequest,
						   HttpServletRequest  request,
						   HttpServletResponse response) throws Exception {

		final String path = decode(request.getParameter("path"));
		final File   file = new File(path); // Can we see the file using the local file system?
		if (!file.exists()) throw new IOException("Path '"+path+"' does not exist!");
		
		final IDataHolder holder = ServiceHolder.getLoaderService().getData(path, new IMonitor.Stub()); // TOOD Make it cancellable?
		
		final String  dataset = decode(request.getParameter("dataset"));
		final ILazyDataset lz = dataset!=null 
				              ? holder.getLazyDataset(dataset)
				              : holder.getLazyDataset(0);
	
		final String slice  = decode(request.getParameter("slice"));		
		final Slice[] sa    = slice!=null ? Slice.convertFromString(slice) : null;
		IDataset data = sa!=null ? lz.getSlice(sa) : null;
		
		// We might load all the data if it is not too large
		if (data==null && lz.getRank()<3) data = lz.getSlice(); // Loads all data
		
		if (data==null) throw new Exception("Cannot get slice of data for '"+path+"'");
		
		data = data.squeeze();
		
		// We downsample if there was one
		String bin = decode(request.getParameter("bin"));
		if (bin!=null) {
			data = ServiceHolder.getDownService().downsample(bin, data).get(0);
		}

		// We set the meta data as header an
		Format format = Format.getFormat(decode(request.getParameter("format")));
		switch(format) {
		case DATA:
			sendObject(data, baseRequest, response);
			break;
		
		case JPG:
		case PNG:
			sendImage(data, baseRequest, request, response, format);
		}
	}
	
	private void sendObject(IDataset            data, 
							Request             baseRequest,
							HttpServletResponse response) throws Exception {

		response.setContentType("application/zip");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);

		response.setHeader("elementClass", data.elementClass().toString());

		final ObjectOutputStream ostream = new ObjectOutputStream(response.getOutputStream());
		try {
			Object buffer = ((Dataset)data).getBuffer();
			ostream.writeObject(buffer);
			ostream.writeObject(data.getShape());
			ostream.writeObject(data.getMetadata());

		} finally {
			ostream.flush();
			ostream.close();
		}
	}

	private void sendImage(IDataset            data, 
			               Request             baseRequest,
						   HttpServletRequest  request,
			               HttpServletResponse response, 
			               Format              format) throws Exception {
		
		if (data.getRank()!=2 && data.getRank()!=1) {
			throw new Exception("The data used to make an image must either be 1D or 2D!"); 
		}
			
		response.setContentType("image/jpeg");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);

		IImageService service = ServiceHolder.getImageService();
		ImageServiceBean bean = createImageServiceBean();
		
		String histo = decode(request.getParameter("histo"));
		if (histo!=null) bean.decode(histo);

		bean.setImage(data);
		
		final ImageData    imdata = service.getImageData(bean);
		final BufferedImage image = service.getBufferedImage(imdata);
		
		ImageIO.write(image, format.getImageIOString(), response.getOutputStream());		
	}
	
	
	private ImageServiceBean createImageServiceBean() {
		ImageServiceBean imageServiceBean = new ImageServiceBean();
		imageServiceBean.setPalette(makeGrayScalePalette());
		imageServiceBean.setOrigin(ImageOrigin.TOP_LEFT);
		imageServiceBean.setMinimumCutBound(HistogramBound.DEFAULT_MINIMUM);
		imageServiceBean.setMaximumCutBound(HistogramBound.DEFAULT_MAXIMUM);
		imageServiceBean.setNanBound(HistogramBound.DEFAULT_NAN);
		
		imageServiceBean.setHistogramType(HistoType.OUTLIER_VALUES);
		imageServiceBean.setLo(5);
		imageServiceBean.setHi(95);		
		
		return imageServiceBean;
	}
	/**
	 * Make 256 level grayscale palette.
	 */
	public static PaletteData makeGrayScalePalette() {
		RGB grayscale[] = new RGB[256];
		for (int i = 0; i < 256; i++) {
			grayscale[i] = new RGB(i, i, i);
		}
		return new PaletteData(grayscale);
	}


	private String decode(String value) throws UnsupportedEncodingException {
		if (value==null) return null;
		return URLDecoder.decode(value, "UTF-8");
	}

	@Override
	public void valueBound(HttpSessionBindingEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void valueUnbound(HttpSessionBindingEvent event) {
		// TODO Auto-generated method stub
		
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((sessionId == null) ? 0 : sessionId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SliceRequest other = (SliceRequest) obj;
		if (sessionId == null) {
			if (other.sessionId != null)
				return false;
		} else if (!sessionId.equals(other.sessionId))
			return false;
		return true;
	}

	public void start() {
		System.out.println(">>>>>> Slice Request Started");
	}
	public void stop() {
		System.out.println(">>>>>> Slice Request Stopped");
	}
}
