/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package uk.ac.ccp4.commandserver.mrbump.beans;

import org.dawnsci.commandserver.core.beans.StatusBean;

/**
 * Bean to serialise with JSON and be sent to the server.
 * 
 * JSON is used rather than the direct object because we may want to have
 * a python server.
 * 
 * @author Matthew Gerring
 *
 */
public class BumpBean extends StatusBean {

	private String      mtzFile;              
	private String      sequenceFile;              
	
	public BumpBean(){
	}

	@Override
	public void merge(StatusBean with) {
        super.merge(with);
        BumpBean db = (BumpBean)with;
        this.mtzFile      = db.mtzFile;
        this.sequenceFile = db.sequenceFile;
	}

	public String getMtzFile() {
		return mtzFile;
	}

	public void setMtzFile(String mtzFile) {
		this.mtzFile = mtzFile;
	}

	public String getSequenceFile() {
		return sequenceFile;
	}

	public void setSequenceFile(String sequenceFile) {
		this.sequenceFile = sequenceFile;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((mtzFile == null) ? 0 : mtzFile.hashCode());
		result = prime * result
				+ ((sequenceFile == null) ? 0 : sequenceFile.hashCode());
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
		BumpBean other = (BumpBean) obj;
		if (mtzFile == null) {
			if (other.mtzFile != null)
				return false;
		} else if (!mtzFile.equals(other.mtzFile))
			return false;
		if (sequenceFile == null) {
			if (other.sequenceFile != null)
				return false;
		} else if (!sequenceFile.equals(other.sequenceFile))
			return false;
		return true;
	}
	
}
