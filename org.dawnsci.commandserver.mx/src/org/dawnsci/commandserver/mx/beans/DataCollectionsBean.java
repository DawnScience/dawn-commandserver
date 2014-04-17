package org.dawnsci.commandserver.mx.beans;

import java.util.ArrayList;
import java.util.List;

import org.dawnsci.commandserver.core.StatusBean;

/**
 * Bean to serialise with JSON and be sent to the server.
 * 
 * JSON is used rather than the direct object because we may want to have
 * a python server.
 * 
 * @author fcp94556
 *
 */
public class DataCollectionsBean extends StatusBean {

	private List<DataCollectionBean> collections;
	private String                   statusQueueName;
	private String                   userName;
	
	public DataCollectionsBean(){
	}

	public List<DataCollectionBean> getCollections() {
		return collections;
	}

	public void setCollections(List<DataCollectionBean> collections) {
		this.collections = collections;
	}

	public void addCollection(DataCollectionBean bean) {
		if (collections==null) collections = new ArrayList<DataCollectionBean>(7);
		collections.add(bean);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((collections == null) ? 0 : collections.hashCode());
		result = prime * result
				+ ((statusQueueName == null) ? 0 : statusQueueName.hashCode());
		result = prime * result
				+ ((userName == null) ? 0 : userName.hashCode());
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
		DataCollectionsBean other = (DataCollectionsBean) obj;
		if (collections == null) {
			if (other.collections != null)
				return false;
		} else if (!collections.equals(other.collections))
			return false;
		if (statusQueueName == null) {
			if (other.statusQueueName != null)
				return false;
		} else if (!statusQueueName.equals(other.statusQueueName))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}
	/**
	 *  Creates name based on all the data collections.
	 */
	public void createName() {
		DataCollectionBean start = collections.get(0);
		DataCollectionBean end   = collections.get(collections.size()-1);
		name = start.getName()+" - "+end.getName();
	}
	public String getStatusQueueName() {
		return statusQueueName;
	}
	public void setStatusQueueName(String statusQueueName) {
		this.statusQueueName = statusQueueName;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
}
