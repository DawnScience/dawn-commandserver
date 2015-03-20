package org.dawnsci.slice.server;

public enum Format {

	DATA, JPG, PNG, MJPG(0);
	
	/**
	 * The dimension to slice over when doing MJPG streams.
	 */
	private int dimension;

	public int getDimension() {
		return dimension;
	}
	public void setDimension(int dimension) {
		this.dimension = dimension;
	}
	
	public boolean isImage() {
		return this==JPG || this==PNG;
	}
	
	Format() {
		this(0);
	}
	Format(int dimension) {
		this.dimension=dimension;
	}
	
	public static Format getFormat(String value) {
		if (value == null) return DATA;
		
		if (value.indexOf(':')>-1) {
			String[] sa = value.split("\\:");
			Format f = valueOf(sa[0]);
			f.setDimension(Integer.parseInt(sa[1]));
			return f;
		}
		return valueOf(value);
	}

	public String getImageIOString() {
		switch(this) {
		case JPG:
		case PNG:
		case MJPG:
			return toString().toLowerCase();
		default:
			throw new RuntimeException("ImageIO not supported with format: "+this);
		}
	}
}
