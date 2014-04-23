package org.dawnsci.commandserver.ui;

import java.text.DateFormat;
import java.util.Date;

import javax.jms.TextMessage;

import org.dawnsci.commandserver.core.Status;
import org.dawnsci.commandserver.core.StatusBean;

class QueueObject {
	
	/**
	 * 
	 */
	private DateFormat format = DateFormat.getDateTimeInstance();
	private TextMessage message;
	private StatusBean  bean;
	public TextMessage getMessage() {
		return message;
	}
	public String getSubmissionDate() throws Exception {
		final long sub = message.getJMSTimestamp();
		return format.format(new Date(sub));
	}
	public double getPercentComplete() {
		return bean.getPercentComplete();
	}
	public Status getStatus() {
		return bean.getStatus();
	}
	public String getName() {
		return bean.getName();
	}
	public void setMessage(TextMessage message) {
		this.message = message;
	}
	public StatusBean getBean() {
		return bean;
	}
	public void setBean(StatusBean bean) {
		this.bean = bean;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bean == null) ? 0 : bean.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
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
		QueueObject other = (QueueObject) obj;
		if (bean == null) {
			if (other.bean != null)
				return false;
		} else if (!bean.equals(other.bean))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}
	public QueueObject(TextMessage message, StatusBean bean) {
		super();
		this.message = message;
		this.bean = bean;
	}
	
	public void merge(TextMessage tm, StatusBean bean) {
		this.message = tm;
		this.bean.merge(bean);
	}
	
}