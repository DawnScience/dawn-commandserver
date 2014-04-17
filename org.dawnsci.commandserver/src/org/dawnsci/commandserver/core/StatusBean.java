package org.dawnsci.commandserver.core;


/**
 * A bean whose JSON value can sit in a queue on the JMS server and 
 * provide information about state.
 * 
 * @author fcp94556
 *
 */
public class StatusBean {

	/**
	 * The uid is generally provided by the client
	 * then returned back from the server to know
	 * which run we are talking about.
	 */
	protected Status status;
	protected String name;
	protected double percentComplete;
	
	public StatusBean() {		
		this.status          = Status.SUBMITTED;
		this.percentComplete = 0;
	}
	
	public StatusBean(String name) {
		this();
		this.name = name;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public double getPercentComplete() {
		return percentComplete;
	}
	public void setPercentComplete(double percentComplete) {
		this.percentComplete = percentComplete;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		long temp;
		temp = Double.doubleToLongBits(percentComplete);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		StatusBean other = (StatusBean) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (Double.doubleToLongBits(percentComplete) != Double
				.doubleToLongBits(other.percentComplete))
			return false;
		if (status != other.status)
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
