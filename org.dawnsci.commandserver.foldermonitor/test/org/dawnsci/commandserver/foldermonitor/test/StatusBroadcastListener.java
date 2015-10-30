package org.dawnsci.commandserver.foldermonitor.test;

import org.eclipse.scanning.api.event.status.StatusBean;

public interface StatusBroadcastListener {

	void statusBroadcasted(StatusBean bean);
}
