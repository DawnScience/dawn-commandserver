package org.dawnsci.commandserver.workflow;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.dawb.workbench.jmx.service.IWorkflowService;
import org.dawb.workbench.jmx.service.WorkflowFactory;
import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;

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
		
		File newFile = null;
		if (bean.getProperties().containsKey("file_path")) {
			newFile = new File(bean.getProperty("file_path"));
		} else {
			newFile = File.createTempFile("default", ".data_input");
		}


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
		
		// Normally is it not blocking, many workflows may run at the same time.
		if (isBlocking()) {
			workflow.waitFor(); // Waits until it is finished.
			// Release any memory used by the object
			service.clear();
			
			bean.setStatus(Status.COMPLETE);
			bean.setPercentComplete(100d);
			bean.setMessage("Ran "+bean.getProperty("momlLocation"));
			broadcast(bean);
		}
	}

	@Override
	public void terminate() throws Exception {
		if (service!=null) service.stop(-101);
	}

	protected IWorkflowService getService() {
		return service;
	}

}
