package org.dawnsci.commandserver.ui.view;

import org.dawnsci.commandserver.core.beans.StatusBean;

public interface IRerunHandler {

	/**
	 * Defines if this handler can open the result in this bean.
	 * @param bean
	 * @return
	 */
	public boolean isHandled(StatusBean bean);
	
	/**
	 * Called to open the result from the beam.
	 * @param bean
	 * @throws Exception if something unexpected goes wrong
	 * @return true if result open handled ok, false otherwise.
	 */
	public boolean run(StatusBean bean) throws Exception;
}
