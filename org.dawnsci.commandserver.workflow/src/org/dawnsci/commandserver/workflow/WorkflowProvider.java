package org.dawnsci.commandserver.workflow;

import java.io.File;
import java.util.Properties;

import org.dawb.workbench.jmx.IRemoteServiceProvider;
import org.dawb.workbench.jmx.IRemoteWorkbench;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.eclipse.core.resources.ResourcesPlugin;

public class WorkflowProvider implements IRemoteServiceProvider {

	private StatusBean       bean;
	private IRemoteWorkbench bench;

	public WorkflowProvider(ProgressableProcess broadcaster, StatusBean bean) {
		this.bench       = new WorkflowProxy(broadcaster, bean);
		this.bean        = bean;
	}

	@Override
	public IRemoteWorkbench getRemoteWorkbench() throws Exception {
		return bench;
	}

	@Override
	public int getStartPort() {
		return 8690;
	}

	@Override
	public String getWorkspacePath() {
		final String location = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		return location;
	}

	@Override
	public String getModelPath() {
		return bean.getProperty("momlLocation");
	}

	@Override
	public String getInstallationPath() {
		// For instance at Diamond:   "module load dawn/snapshot ; $DAWN_RELEASE_DIRECTORY/dawn"
		// Or on windows: "C:\Users\fcp94556\Desktop\DawnMaster\dawn.exe"
		return bean.getProperty("execLocation");
	}

	@Override
	public boolean getServiceTerminate() {
		return false;
	}

	@Override
	public boolean getTangoSpecMockMode() {
		return true;
	}

	@Override
	public Properties getProperties() {
		return bean.getProperties();
	}

}
