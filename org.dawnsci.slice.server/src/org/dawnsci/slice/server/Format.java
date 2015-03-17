package org.dawnsci.slice.server;

public enum Format {

	DATA, JPG, PNG;
	
	public static Format getFormat(String value) {
		if (value == null) return DATA;
		return valueOf(value);
	}
}
