/*
 * Copyright (c) 2014 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.ptycho.beans;

import org.dawnsci.commandserver.core.beans.StatusBean;

/**
 * Bean to serialise with JSON and be sent to the server.
 * 
 * JSON is used rather than the direct object because we may want to have a
 * python server.
 * 
 * 
 */
public class PtychoBean extends StatusBean {

	private String projectName;
	private String fileName;
	private String pythonCode;
	private String pythonClass;

	public PtychoBean() {
	}

	@Override
	public void merge(StatusBean with) {
		super.merge(with);
		PtychoBean db = (PtychoBean) with;
		this.projectName = db.projectName;
		this.fileName = db.fileName;
		this.pythonCode = db.pythonCode;
	}

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getPythonCode() {
		return pythonCode;
	}

	public void setPythonCode(String pythonCode) {
		this.pythonCode = pythonCode;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getPythonClass() {
		return pythonClass;
	}

	public void setPythonClass(String pythonClass) {
		this.pythonClass = pythonClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result
				+ ((projectName == null) ? 0 : projectName.hashCode());
		result = prime * result
				+ ((pythonCode == null) ? 0 : pythonCode.hashCode());
		result = prime * result
				+ ((pythonClass == null) ? 0 : pythonClass.hashCode());
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
		PtychoBean other = (PtychoBean) obj;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (projectName == null) {
			if (other.projectName != null)
				return false;
		} else if (!projectName.equals(other.projectName))
			return false;
		if (pythonCode == null) {
			if (other.pythonCode != null)
				return false;
		} else if (!pythonCode.equals(other.pythonCode))
			return false;
		if (pythonClass == null) {
			if (other.pythonClass != null)
				return false;
		} else if (!pythonClass.equals(other.pythonClass))
			return false;
		return true;
	}
}
