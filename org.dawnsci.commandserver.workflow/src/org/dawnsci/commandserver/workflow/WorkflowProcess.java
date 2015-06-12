package org.dawnsci.commandserver.workflow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.dawb.workbench.jmx.service.IWorkflowService;
import org.dawb.workbench.jmx.service.WorkflowFactory;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.foldermonitor.FolderEventBean;

public class WorkflowProcess extends ProgressableProcess {

	private File    tmpDir;
	private IWorkflowService service;

	public WorkflowProcess(URI                uri, 
			               final String       processName,
	                       String             statusTName, 
	                       String             statusQName, 			           
	                       Map<String,String> arguments,
                           StatusBean         sbean) throws IOException {
		
		super(uri, statusTName, statusQName, sbean);
		
		final FolderEventBean bean = (FolderEventBean)sbean;
		
		final File newFile = new File(bean.getPath());
		if (!newFile.exists()) throw new FileNotFoundException("Cannot find "+bean.getPath());
		
		final File visitDir = newFile.getParentFile().getParentFile();

		// We run the commands in a temporary directory for this consumer
		tmpDir = getUnique(new File(visitDir, "tmp"), processName+"processing_", 1);
 		bean.setRunDirectory(tmpDir.getAbsolutePath());
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(tmpDir, processName+"Bean.json");
		} catch (Exception e) {
			e.printStackTrace(out);
		}
		
		// If we have the -scriptLocation argument, use that		
		String momlLocation = arguments.get("momlLocation");
		if (momlLocation==null || "null".equals(momlLocation)) throw new IOException("-momlLocation argument must be set");
		bean.setProperty("momlLocation", momlLocation);
		
		String execLocation = arguments.get("execLocation");
		if (execLocation==null || "null".equals(momlLocation)) throw new IOException("-execLocation argument must be set to location of dawn executable to use for the workflow!");
		bean.setProperty("execLocation", execLocation);

	}

	@Override
	public void execute() throws Exception {
		
        this.service = WorkflowFactory.createWorkflowService(new WorkflowProvider(this, bean));
		final Process workflow = service.start();
		if (isBlocking()) workflow.waitFor(); // Waits until it is finished.
		
		// Release any memory used by the object
		service.clear();
	}

	@Override
	public void terminate() throws Exception {
		if (service!=null) service.stop(-101);
	}

}
