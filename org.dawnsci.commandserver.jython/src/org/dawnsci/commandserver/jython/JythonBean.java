package org.dawnsci.commandserver.jython;

import org.dawnsci.commandserver.core.beans.StatusBean;

public class JythonBean extends StatusBean {

	private String jythonClass;

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
