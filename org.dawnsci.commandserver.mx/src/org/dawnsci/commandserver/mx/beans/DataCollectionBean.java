package org.dawnsci.commandserver.mx.beans;

import java.util.List;

/**
 * Bean holds information about a given data collections participation in
 * the auto-processing rerun.
 * 
 * @author fcp94556
 *
 */
public class DataCollectionBean {

	// TODO Not sure which other fields are required for xia2 rerun.
	private String        name;
	private String        dataCollectionId;
	private List<String>  slices;
	
	public DataCollectionBean() {
		
	}
	public DataCollectionBean(String name, String dataCollectionId, List<String> slices) {
		this.name             = name;
		this.dataCollectionId = dataCollectionId;
		this.slices           = slices;
	}
	
	public String getDataCollectionId() {
		return dataCollectionId;
	}
	public void setDataCollectionId(String dataCollectionId) {
		this.dataCollectionId = dataCollectionId;
	}
	public List<String> getSlices() {
		return slices;
	}
	public void setSlices(List<String> slices) {
		this.slices = slices;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((dataCollectionId == null) ? 0 : dataCollectionId.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((slices == null) ? 0 : slices.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataCollectionBean other = (DataCollectionBean) obj;
		if (dataCollectionId == null) {
			if (other.dataCollectionId != null)
				return false;
		} else if (!dataCollectionId.equals(other.dataCollectionId))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (slices == null) {
			if (other.slices != null)
				return false;
		} else if (!slices.equals(other.slices))
			return false;
		return true;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
