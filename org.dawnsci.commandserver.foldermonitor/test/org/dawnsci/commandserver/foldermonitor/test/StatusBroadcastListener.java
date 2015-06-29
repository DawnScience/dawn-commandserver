package org.dawnsci.commandserver.foldermonitor.test;

import org.dawnsci.commandserver.core.beans.StatusBean;

public interface StatusBroadcastListener {

	void statusBroadcasted(StatusBean bean);
}
