package org.dawnsci.slice.server;

public enum Format {

	DATA, JPG, PNG;
	
	public static Format getFormat(String value) {
		if (value == null) return DATA;
		return valueOf(value);
	}

	public String getImageIOString() {
		switch(this) {
		case JPG:
		case PNG:
			return toString().toLowerCase();
		default:
			throw new RuntimeException("ImageIO not supported with format: "+this);
		}
	}
}
