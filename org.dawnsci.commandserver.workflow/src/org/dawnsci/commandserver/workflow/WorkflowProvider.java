package org.dawnsci.commandserver.workflow;

import java.util.Properties;

import org.dawb.workbench.jmx.IRemoteServiceProvider;
import org.dawb.workbench.jmx.IRemoteWorkbench;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.eclipse.core.resources.ResourcesPlugin;

public class WorkflowProvider implements IRemoteServiceProvider {

	private StatusBean       bean;
	private IRemoteWorkbench bench;

	public WorkflowProvider(WorkflowProcess process, StatusBean bean) {
		this.bench       = new WorkflowProxy(process, bean);
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
		if (bean.getProperties()!=null && bean.getProperties().containsKey("workspaceLocation")) {
			return bean.getProperty("workspaceLocation");
		}
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
		if (isWindowsOS() && bean.getProperty("winExecLocation")!=null) {
			return bean.getProperty("winExecLocation");
		}
		return bean.getProperty("execLocation");
	}
	
	/**
	 * @return true if windows
	 */
	static public final boolean isWindowsOS() {
		return (System.getProperty("os.name").indexOf("Windows") == 0);
	}

	@Override
	public boolean getServiceTerminate() {
		return true;
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
