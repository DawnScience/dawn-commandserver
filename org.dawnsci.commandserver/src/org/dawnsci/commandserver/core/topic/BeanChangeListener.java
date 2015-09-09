package org.dawnsci.commandserver.core.topic;

import java.util.EventListener;

public interface BeanChangeListener<T> extends EventListener {

	/**
	 * Called to notify that a given bean changed.
	 * @param event
	 */
	public void beanChangePerformed(BeanChangeEvent<T> event);
}
