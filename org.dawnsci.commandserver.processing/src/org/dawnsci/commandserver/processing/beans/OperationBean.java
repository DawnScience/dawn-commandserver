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

import org.eclipse.dawnsci.analysis.api.dataset.SliceND;
import org.eclipse.dawnsci.analysis.api.processing.ExecutionType;
import org.eclipse.scanning.api.event.status.StatusBean;

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
	
	// The data
	private String               filePath;              
	private String               datasetPath;
	private SliceND slicing;
	private Map<Integer, String> axesNames;
	private String 				 outputFilePath;
	private int[] 				 dataDimensions;
	
	// The pipeline that we need to run
	// The pipeline is saved shared disk at
	// the moment in order to be run on the cluster.
	// This is not ideal because if the consumer 
	// and client do not share disk, it will not work.
	private String               persistencePath;

	private ExecutionType        executionType=ExecutionType.SERIES;
	private long                 parallelTimeout=5000;
	private String 				 xmx;
	private boolean				 readable = false;
	
	// Tidying stuff
	private boolean deletePersistenceFile = true;
	
	public OperationBean(){
		
	}

	@Override
	public void merge(StatusBean with) {
        super.merge(with);
        OperationBean db = (OperationBean)with;
        this.filePath        = db.filePath;
        this.datasetPath     = db.datasetPath;
        this.slicing         = db.slicing;
        this.persistencePath = db.persistencePath;
        this.executionType   = db.executionType;
        this.parallelTimeout = db.parallelTimeout;
        this.deletePersistenceFile = db.deletePersistenceFile;
        this.xmx = db.xmx;
	}
	

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String fileName) {
		this.filePath = fileName;
	}
	
	public String getOutputFilePath() {
		return outputFilePath;
	}

	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	public String getDatasetPath() {
		return datasetPath;
	}

	public void setDatasetPath(String datasetPath) {
		this.datasetPath = datasetPath;
	}

	public SliceND getSlicing() {
		return slicing;
	}

	public void setSlicing(SliceND slicing) {
		this.slicing = slicing;
	}

	public String getPersistencePath() {
		return persistencePath;
	}

	public void setPersistencePath(String persistencePath) {
		this.persistencePath = persistencePath;
	}
	
	public void setAxesNames(Map<Integer, String> axesNames) {
		this.axesNames = axesNames;
	}
	
	public Map<Integer, String> getAxesNames() {
		return this.axesNames;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((datasetPath == null) ? 0 : datasetPath.hashCode());
		result = prime * result + (deletePersistenceFile ? 1231 : 1237);
		result = prime * result
				+ ((executionType == null) ? 0 : executionType.hashCode());
		result = prime * result
				+ ((filePath == null) ? 0 : filePath.hashCode());
		result = prime * result
				+ (int) (parallelTimeout ^ (parallelTimeout >>> 32));
		result = prime * result
				+ ((persistencePath == null) ? 0 : persistencePath.hashCode());
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
		if (deletePersistenceFile != other.deletePersistenceFile)
			return false;
		if (executionType != other.executionType)
			return false;
		if (filePath == null) {
			if (other.filePath != null)
				return false;
		} else if (!filePath.equals(other.filePath))
			return false;
		if (parallelTimeout != other.parallelTimeout)
			return false;
		if (persistencePath == null) {
			if (other.persistencePath != null)
				return false;
		} else if (!persistencePath.equals(other.persistencePath))
			return false;
		if (slicing == null) {
			if (other.slicing != null)
				return false;
		} else if (!slicing.equals(other.slicing))
			return false;
		return true;
	}

	public ExecutionType getExecutionType() {
		return executionType;
	}

	public void setExecutionType(ExecutionType executionType) {
		this.executionType = executionType;
	}

	public long getParallelTimeout() {
		return parallelTimeout;
	}

	public void setParallelTimeout(long parallelTimeout) {
		this.parallelTimeout = parallelTimeout;
	}

	public boolean isDeletePersistenceFile() {
		return deletePersistenceFile;
	}

	public void setDeletePersistenceFile(boolean deletePersistenceFile) {
		this.deletePersistenceFile = deletePersistenceFile;
	}

	public String getXmx() {
		return xmx;
	}

	public void setXmx(String xmx) {
		this.xmx = xmx;
	}

	public int[] getDataDimensions() {
		return dataDimensions;
	}

	public void setDataDimensions(int[] dataDimensions) {
		this.dataDimensions = dataDimensions;
	}

	public boolean isReadable() {
		return readable;
	}

	public void setReadable(boolean readable) {
		this.readable = readable;
	}
}
