package org.dawnsci.commandserver.bubbles;

import org.eclipse.scanning.api.event.status.StatusBean;


/**
 * The bean to be used in the queue for 
 * monitoring status and submitting a job.
 * 
 * @author Matthew Gerring
 *
 */
public class BubblesBean extends StatusBean {

	private String intensityPath;
	private String pofrPath;
	
	@Override
	public void merge(StatusBean with) {

		super.merge(with);
		
		BubblesBean bwith = (BubblesBean)with;
		this.intensityPath = bwith.intensityPath;
		this.pofrPath      = bwith.pofrPath;		
	}
	
	public String getIntensityPath() {
		return intensityPath;
	}
	public void setIntensityPath(String intensityPath) {
		this.intensityPath = intensityPath;
	}
	public String getPofrPath() {
		return pofrPath;
	}
	public void setPofrPath(String pofrPath) {
		this.pofrPath = pofrPath;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((intensityPath == null) ? 0 : intensityPath.hashCode());
		result = prime * result
				+ ((pofrPath == null) ? 0 : pofrPath.hashCode());
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
		BubblesBean other = (BubblesBean) obj;
		if (intensityPath == null) {
			if (other.intensityPath != null)
				return false;
		} else if (!intensityPath.equals(other.intensityPath))
			return false;
		if (pofrPath == null) {
			if (other.pofrPath != null)
				return false;
		} else if (!pofrPath.equals(other.pofrPath))
			return false;
		return true;
	}
}
