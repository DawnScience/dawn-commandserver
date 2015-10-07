package org.dawnsci.commandserver.core.topic;

import java.util.EventObject;

public class BeanChangeEvent<T> extends EventObject {

	private static final long serialVersionUID = 2504218539218704939L;

	public BeanChangeEvent(T source) {
		super(source);
	}

	@SuppressWarnings("unchecked")
	public T getBean() {
		return (T)getSource();
	}
}
