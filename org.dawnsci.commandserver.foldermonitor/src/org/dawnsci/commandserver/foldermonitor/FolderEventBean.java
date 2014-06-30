package org.dawnsci.commandserver.foldermonitor;

import org.dawnsci.commandserver.core.beans.StatusBean;

public class FolderEventBean extends StatusBean {

	private EventType type;
	private String   path;
	
	public FolderEventBean(EventType type, String path) {
		this.type = type;
		this.path = path;
	}
	
}
