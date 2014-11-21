/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.processing.beans;

import java.util.Map;

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
public class OperationBean extends StatusBean {

	// The name of the pipeline to run (used in the run directory)
	private String               pipelineName;
	
	// The data
	private String               fileName;              
	private String               datasetPath;
	private Map<Integer, String> slicing;
	
	// The pipeline that we need to run
	// The pipeline is saved shared disk at
	// the moment in order to be run on the cluster.
	// This is not ideal because if the consumer 
	// and client do not share disk, it will not work.
	private String               persistencePath;
	
	public OperationBean(){
		
	}

	@Override
	public void merge(StatusBean with) {
        super.merge(with);
        OperationBean db = (OperationBean)with;
        this.pipelineName    = db.pipelineName;
        this.fileName        = db.fileName;
        this.datasetPath     = db.datasetPath;
        this.slicing         = db.slicing;
        this.persistencePath = db.persistencePath;
	}
	

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getDatasetPath() {
		return datasetPath;
	}

	public void setDatasetPath(String datasetPath) {
		this.datasetPath = datasetPath;
	}

	public Map<Integer, String> getSlicing() {
		return slicing;
	}

	public void setSlicing(Map<Integer, String> slicing) {
		this.slicing = slicing;
	}

	public String getPersistencePath() {
		return persistencePath;
	}

	public void setPersistencePath(String persistencePath) {
		this.persistencePath = persistencePath;
	}

	public String getPipelineName() {
		return pipelineName;
	}

	public void setPipelineName(String pipelineName) {
		this.pipelineName = pipelineName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((datasetPath == null) ? 0 : datasetPath.hashCode());
		result = prime * result
				+ ((fileName == null) ? 0 : fileName.hashCode());
		result = prime * result
				+ ((persistencePath == null) ? 0 : persistencePath.hashCode());
		result = prime * result
				+ ((pipelineName == null) ? 0 : pipelineName.hashCode());
		result = prime * result + ((slicing == null) ? 0 : slicing.hashCode());
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
		OperationBean other = (OperationBean) obj;
		if (datasetPath == null) {
			if (other.datasetPath != null)
				return false;
		} else if (!datasetPath.equals(other.datasetPath))
			return false;
		if (fileName == null) {
			if (other.fileName != null)
				return false;
		} else if (!fileName.equals(other.fileName))
			return false;
		if (persistencePath == null) {
			if (other.persistencePath != null)
				return false;
		} else if (!persistencePath.equals(other.persistencePath))
			return false;
		if (pipelineName == null) {
			if (other.pipelineName != null)
				return false;
		} else if (!pipelineName.equals(other.pipelineName))
			return false;
		if (slicing == null) {
			if (other.slicing != null)
				return false;
		} else if (!slicing.equals(other.slicing))
			return false;
		return true;
	}
}
