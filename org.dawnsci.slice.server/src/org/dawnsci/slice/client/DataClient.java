package org.dawnsci.slice.client;

import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.dawnsci.slice.server.Format;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.metadata.IMetadata;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;
/**
 *   
 *    Class to look after making a connection to the HTTP Data slice server.
 *    Basically it encodes the parameters if a GET is used or if a POST is used,
 *    it deals with that. There is no need when using the client to know how the 
 *    HTTP connection is managed.
 *   
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
 *    bin     - downsample  As in Downsample.fromString(...) ; examples: 'MEAN:2x3', 'MAXIMUM:2x2' 
 *              by default no downsampling is done
 *              
 *    format  - One of Format.values():
 *              DATA - zipped slice, binary (default)
 *              JPG  - JPG made using IImageService to make the image
 *              PNG  - PNG made using IImageService to make the image
 * 
 *    `URL encoded.

 * @author fcp94556
 *
 */
public class DataClient {

	private String     base;
	private String     path;
	private String     dataset;
	private String     slice;
	private String     bin;
	private Format     format;
	
	
	public DataClient(String base) {
		this.base = base;
	}
	
	public IDataset getData() throws Exception {
		
		if (format!=null && format!=Format.DATA) {
			throw new Exception("Cannot get data with format set to "+format);
		}
		
		final URL url = new URL(getURLString());
		URLConnection  conn = url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);

        ObjectInputStream oin=null;
		try {
	        oin  = new ObjectInputStream(url.openStream());
			
			Object buffer = oin.readObject();
			Object shape  = oin.readObject();
			Object meta   = oin.readObject();
			
			IDataset ret = DatasetFactory.createFromObject(buffer);
			ret.setShape((int[])shape);
			ret.setMetadata((IMetadata)meta);
			return ret;
			
		} finally {
			if (oin!=null) oin.close();
 		}

	}
	
	
	private String getURLString() throws Exception {
		final StringBuilder buf = new StringBuilder();
		buf.append(base);
		buf.append("?");
		append(buf, "path",    path);
		append(buf, "dataset", dataset);
		append(buf, "slice",   slice);
		append(buf, "bin",     bin);
	    append(buf, "format",  format);
		return buf.toString();
	}

	private void append(StringBuilder buf, String name, Object object) throws UnsupportedEncodingException {
		if (object==null || "".equals(object)) return;
		
		String value = object.toString();
		buf.append(name);
		buf.append("=");
		buf.append(URLEncoder.encode(value, "UTF-8"));
		buf.append("&");
	}

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getDataset() {
		return dataset;
	}
	public void setDataset(String dataset) {
		this.dataset = dataset;
	}
	public String getSlice() {
		return slice;
	}
	public void setSlice(String slice) {
		this.slice = slice;
	}
	public String getBin() {
		return bin;
	}

	public void setBin(String bin) {
		this.bin = bin;
	}

	public Format getFormat() {
		return format;
	}
	public void setFormat(Format format) {
		this.format = format;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result + ((bin == null) ? 0 : bin.hashCode());
		result = prime * result + ((dataset == null) ? 0 : dataset.hashCode());
		result = prime * result + ((format == null) ? 0 : format.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((slice == null) ? 0 : slice.hashCode());
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
		DataClient other = (DataClient) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (bin == null) {
			if (other.bin != null)
				return false;
		} else if (!bin.equals(other.bin))
			return false;
		if (dataset == null) {
			if (other.dataset != null)
				return false;
		} else if (!dataset.equals(other.dataset))
			return false;
		if (format != other.format)
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (slice == null) {
			if (other.slice != null)
				return false;
		} else if (!slice.equals(other.slice))
			return false;
		return true;
	}
	
	
}
