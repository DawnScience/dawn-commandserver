package org.dawnsci.commandserver.workflow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.Map;

import org.dawb.workbench.jmx.service.IWorkflowService;
import org.dawb.workbench.jmx.service.WorkflowFactory;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.isencia.passerelle.workbench.model.launch.ModelRunner;

public class WorkflowProcess extends ProgressableProcess {
	
	private static final Logger logger = LoggerFactory.getLogger(WorkflowProcess.class);

	private File             runDir;
	private IWorkflowService service;

	private Map<String, String> arguments;

	public WorkflowProcess(URI                uri, 
			               final String       processName,
	                       String             statusTName, 
	                       String             statusQName, 			           
	                       Map<String,String> arguments,
                           StatusBean         sbean) throws IOException {
		
		super(uri, statusTName, statusQName, sbean);
		this.arguments = arguments;
		
		File newFile = null;
		if (bean.getProperties().containsKey("file_path")) {
			newFile = new File(bean.getProperty("file_path"));
		} else {
			newFile = File.createTempFile("default", ".data_input");
		}


		final File visitDir = newFile.getParentFile().getParentFile();

		// We run the commands in a temporary directory for this consumer
		runDir = getUnique(new File(visitDir, "tmp"), processName+"processing_", 1);
 		bean.setRunDirectory(runDir.getAbsolutePath());
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(runDir, processName+"Bean.json");
		} catch (Exception e) {
			e.printStackTrace(out);
		}
		bean.setProperty("logLocation",  "\""+runDir.getAbsolutePath()+"/workflow_log.txt\"");
	
		// If we have the -scriptLocation argument, use that		
		String momlLocation = arguments.get("momlLocation");
		if (momlLocation==null || "null".equals(momlLocation) || "".equals(momlLocation)) {
			throw new IOException("-momlLocation argument must be set");
		}
		bean.setProperty("momlLocation",  momlLocation);
		bean.setProperty("workflow_name", (new File(momlLocation)).getName());
		
		String execLocation = arguments.get("execLocation");
		if (execLocation==null || "null".equals(execLocation) || "".equals(execLocation)) {
			throw new IOException("-execLocation argument must be set to location of dawn executable to use for the workflow!");
		}
		bean.setProperty("execLocation", execLocation);

		// One windows can have different command to run workflow if we like, optional
		String winExecLocation = arguments.get("winExecLocation");
		if (winExecLocation!=null && !"null".equals(winExecLocation) && !"".equals(winExecLocation)) {
			bean.setProperty("winExecLocation", winExecLocation);
		}
	}
	
	private PrintWriter out, err;

	@Override
	public void execute() throws Exception {
			
        final WorkflowProvider prov = new WorkflowProvider(this, bean);
		
		boolean sameVM = "true".equals(arguments.get("sameVM"));
		if (sameVM) {
	        final ModelRunner runner = new ModelRunner();
	        runner.runModel(prov.getModelPath(), false);
		} else {
			this.service = WorkflowFactory.createWorkflowService(prov);
			
	        this.out = new PrintWriter(new BufferedWriter(new FileWriter(new File(runDir, "workflow_out.txt"))));
	        this.err = new PrintWriter(new BufferedWriter(new FileWriter(new File(runDir, "workflow_err.txt"))));
	        service.setLoggingStreams(out, err);

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
				
				if (out!=null) out.close();
				if (err!=null) err.close();

			}

		}
        
	}

	@Override
	public void terminate() throws Exception {
		if (service!=null) service.stop(-101);
		if (out!=null) out.close();
		if (err!=null) err.close();
	}

	/**
	 * Notify from the workflow service
	 * @param code
	 */
	protected void terminationNotification(int code) {
		if (!isBlocking()) { // We need to clear up
			try {
				service.clear();
			} catch (Exception e) {
				logger.error("Cannot clear service!", e);
			}
		}
		if (out!=null) out.close();
		if (err!=null) err.close();
	}
}
