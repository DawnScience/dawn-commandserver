package org.dawnsci.commandserver.mx.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
public class SubmissionBean extends StatusBean {

	private List<DataCollectionBean> collections;
	private String                   queueName;
	
	public SubmissionBean(){
		this(false);
	}
	public SubmissionBean(boolean generateNewId) {
		super();
		if (generateNewId) {
		    this.uid = System.currentTimeMillis()+"_"+UUID.randomUUID();
		}
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
				+ ((queueName == null) ? 0 : queueName.hashCode());
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
		SubmissionBean other = (SubmissionBean) obj;
		if (collections == null) {
			if (other.collections != null)
				return false;
		} else if (!collections.equals(other.collections))
			return false;
		if (queueName == null) {
			if (other.queueName != null)
				return false;
		} else if (!queueName.equals(other.queueName))
			return false;
		return true;
	}
	public String getQueueName() {
		return queueName;
	}
	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	
	/**
	 *  Creates name based on all the data collections.
	 */
	public void createName() {
		DataCollectionBean start = collections.get(0);
		DataCollectionBean end   = collections.get(collections.size()-1);
		name = start.getName()+" - "+end.getName();
	}	
}
