/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.foldermonitor;

import java.util.Properties;

import org.dawnsci.commandserver.core.beans.StatusBean;

public class FolderEventBean extends StatusBean {

	private EventType  type;
	private String     path;
	private Properties properties;
	
	public FolderEventBean() {
		super();
	}
	
	public FolderEventBean(EventType type, String path) {
		this.type = type;
		this.path = path;
	}
	

	public void merge(FolderEventBean with) {
		super.merge(with);
		this.type   = with.type;
		this.path   = with.path;
		this.properties = with.properties;
	}
       

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FolderEventBean other = (FolderEventBean) obj;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties config) {
		this.properties = config;
	}
	
}
