package org.dawnsci.commandserver.workflow;

import java.util.Map;

import org.dawb.workbench.jmx.ActorSelectedBean;
import org.dawb.workbench.jmx.IRemoteWorkbench;
import org.dawb.workbench.jmx.UserDebugBean;
import org.dawb.workbench.jmx.UserInputBean;
import org.dawb.workbench.jmx.UserPlotBean;
import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkflowProxy implements IRemoteWorkbench {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowProxy.class);
	private StatusBean      bean;
	private WorkflowProcess process;

	public WorkflowProxy(WorkflowProcess process, StatusBean bean) {
		this.process     = process;
		this.bean        = bean;
	}

	@Override
	public void executionStarted() {
		bean.setStatus(Status.RUNNING);
		bean.setPercentComplete(0);
		bean.setMessage("Starting workflow "+bean.getProperty("workflow_name"));
		process.broadcast(bean);
	}

	@Override
	public void executionTerminated(int returnCode) {
		
		bean.setStatus(returnCode==0 ? Status.COMPLETE : Status.FAILED);
		if (returnCode==0) bean.setPercentComplete(100);
		bean.setMessage("Workflow finished with code "+returnCode);
		process.broadcast(bean);
		process.terminationNotification(returnCode);
	}

	@Override
	public boolean openFile(String fullPath) {
		return false;
	}

	@Override
	public boolean monitorDirectory(String fullPath, boolean startMonitoring) {
		return false;
	}

	@Override
	public boolean refresh(String projectName, String resourcePath) {
		return false;
	}

	@Override
	public boolean showMessage(String title, String message, int type) {
		bean.setMessage(message);
		process.broadcast(bean);
		return true;
	}

	@Override
	public void logStatus(String pluginId, String message, Throwable throwable) {
		bean.setMessage(message);
		process.broadcast(bean);
	}

	@Override
	public Map<String, String> createUserInput(UserInputBean bean) throws Exception {
		return bean.getScalar();
	}

	@Override
	public UserPlotBean createPlotInput(UserPlotBean bean) throws Exception {
		return bean;
	}

	@Override
	public UserDebugBean debug(UserDebugBean bean) throws Exception {
		return bean;
	}

	@Override
	public boolean setActorSelected(ActorSelectedBean abean) throws Exception {
		if (abean.isSelected()) {
			bean.setMessage("Running "+abean.getActorName());
			bean.setPercentComplete(bean.getPercentComplete()+1);
			if (bean.getPercentComplete()>99) bean.setPercentComplete(99);
			process.broadcast(bean);
		}
		return abean.isSelected();
	}

	@Override
	public void setMockMotorValue(String motorName, Object value) {

	}

	@Override
	public Object getMockMotorValue(String motorName) {
		return 0d;
	}

	@Override
	public void notifyMockCommand(String motorName, String message, String cmd) {

	}

}
