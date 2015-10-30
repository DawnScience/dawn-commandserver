/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.jython;

import org.eclipse.scanning.api.event.status.StatusBean;

public class JythonBean extends StatusBean {

	private String jythonClass;
	private String jythonCode;
	// This determines whether we're running scripts or raw code (should be true in production)
	private final boolean runScript = true; 

	public boolean getRunScript() {
		return runScript;
	}
	
	public String getJythonCode() {
		return jythonCode;
	}

	public void setJythonCode(String jythonCode) {
		this.jythonCode = jythonCode;
	}

	public String getJythonClass() {
		return jythonClass;
	}

	public void setJythonClass(String jythonClass) {
		this.jythonClass = jythonClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((jythonClass == null) ? 0 : jythonClass.hashCode());
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
		JythonBean other = (JythonBean) obj;
		if (jythonClass == null) {
			if (other.jythonClass != null)
				return false;
		} else if (!jythonClass.equals(other.jythonClass))
			return false;
		return true;
	}
}
