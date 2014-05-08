package org.dawnsci.commandserver.tomo.beans;

import org.dawnsci.commandserver.core.beans.StatusBean;

/**
 * Bean to serialise with JSON and be sent to the server.
 * 
 * JSON is used rather than the direct object because we may want to have
 * a python server.
 * 
 * @author fcp94556
 *
 */
public class TomoBean extends StatusBean {

	private String      projectName;              
	
	public TomoBean(){
	}

	@Override
	public void merge(StatusBean with) {
        super.merge(with);
        TomoBean db = (TomoBean)with;
        this.projectName  = db.projectName;
	}
	


	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((projectName == null) ? 0 : projectName.hashCode());
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
		TomoBean other = (TomoBean) obj;
		if (projectName == null) {
			if (other.projectName != null)
				return false;
		} else if (!projectName.equals(other.projectName))
			return false;
		return true;
	}
}
