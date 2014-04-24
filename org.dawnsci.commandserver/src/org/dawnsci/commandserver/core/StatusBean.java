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
	protected String message; // null or the error message if status is FAILED for instance.
	protected double percentComplete;
	protected String userName;
	
	/**
	 * We intentionally ignore the JMS version of this
	 */
	protected String uniqueId;
	
	/**
	 * We intentionally ignore the JMS version of this
	 */
	protected long   submissionTime;
	
	/**
	 * Subclasses must override this method.
	 * @param with
	 */
	public void merge(StatusBean with) {
		this.status          = with.status;
		this.name            = with.name;
		this.percentComplete = with.percentComplete;
        this.userName        = with.userName;
        this.uniqueId        = with.uniqueId;
        this.submissionTime  = with.submissionTime;
	}

	
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
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		long temp;
		temp = Double.doubleToLongBits(percentComplete);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result
				+ (int) (submissionTime ^ (submissionTime >>> 32));
		result = prime * result
				+ ((uniqueId == null) ? 0 : uniqueId.hashCode());
		result = prime * result
				+ ((userName == null) ? 0 : userName.hashCode());
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
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
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
		if (submissionTime != other.submissionTime)
			return false;
		if (uniqueId == null) {
			if (other.uniqueId != null)
				return false;
		} else if (!uniqueId.equals(other.uniqueId))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}


	public String getName() {
		if (name==null) createName();
		return name;
	}

    /**
     * Override to generate a name automatically.
     */
	protected void createName() {
		// TODO Auto-generated method stub
		
	}

	public void setName(String name) {
		this.name = name;
	}
	

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public long getSubmissionTime() {
		return submissionTime;
	}

	public void setSubmissionTime(long submissionTime) {
		this.submissionTime = submissionTime;
	}


	public String getMessage() {
		return message;
	}


	public void setMessage(String message) {
		this.message = message;
	}


	@Override
	public String toString() {
		return "StatusBean [status=" + status + ", name=" + name + ", message="
				+ message + ", percentComplete=" + percentComplete
				+ ", userName=" + userName + ", uniqueId=" + uniqueId
				+ ", submissionTime=" + submissionTime + "]";
	}
}
