package org.dawnsci.commandserver.foldermonitor;

import org.dawnsci.commandserver.core.beans.StatusBean;

public class FolderEventBean extends StatusBean {

	private EventType type;
	private String    path;
	
	public FolderEventBean(EventType type, String path) {
		this.type = type;
		this.path = path;
	}
	

	public void merge(FolderEventBean with) {
		super.merge(with);
		this.type = with.type;
		this.path = with.path;
	}
       

	protected EventType getType() {
		return type;
	}

	protected void setType(EventType type) {
		this.type = type;
	}

	protected String getPath() {
		return path;
	}

	protected void setPath(String path) {
		this.path = path;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
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
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	
}
