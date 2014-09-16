package org.dawnsci.commandserver.jython;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.process.ProgressableProcess;

public class JythonProcess extends ProgressableProcess {

	private Map<String, String> args;
	private JythonBean          jbean;

	public JythonProcess(URI uri, String statusTName, String statusQName, Map<String, String> args, JythonBean bean) {
		
		super(uri, statusTName, statusQName, bean);
		
		this.args  = args;
		this.jbean = bean;
		
		// We only run one script at a time.
		setBlocking(true);
		
        final String runDir;
		if (isWindowsOS()) {
			// We are likely to be a test consumer, anyway the unix paths
			// from ISPyB will certainly not work, so we process in C:/tmp/
			runDir  = "C:/tmp/"+bean.getRunDirectory();
		} else {
			runDir  = bean.getRunDirectory();
		}

 		final File   jythonDir = getUnique(new File(runDir), "Run_", null, 1);
 		jythonDir.mkdirs();
 		
 		try {
			setLoggingFile(new File(jythonDir, "jythonProcessLog.txt"), true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
				
		bean.setRunDirectory(jythonDir.getAbsolutePath());
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(jythonDir, "jythonRun.json");
		} catch (Exception e) {
			e.printStackTrace(out);
		}

	}

	@Override
	public void execute() throws Exception {
		
		bean.setStatus(Status.RUNNING);
		bean.setPercentComplete(1);
		broadcast(bean);

		// TODO Michael run Jython
        out.println("Run Jython! "+jbean);
        
		bean.setStatus(Status.COMPLETE);
		bean.setMessage("Finished running Jython "+jbean.getJythonClass());
		bean.setPercentComplete(100);
		broadcast(bean);

	}

	@Override
	public void terminate() throws Exception {
		// TODO Auto-generated method stub
	    out.println("terminate Jython!");
	}

}
