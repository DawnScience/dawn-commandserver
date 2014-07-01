package org.dawnsci.commandserver.mrc.consumer;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.util.Map;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.POSIX;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.foldermonitor.FolderEventBean;

/**
 * A process which blocks until done.
 * @author fcp94556
 *
 */
public class MRCProcess extends ProgressableProcess {

	private File    runDir;
	private String  scriptLocation;
	private String  momlLocation;
	private Process process;

	public MRCProcess(URI                uri, 
			          String             statusTName, 
			          String             statusQName, 			           
			          Map<String,String> arguments,
                      StatusBean         bean) throws IOException {
		
		super(uri, statusTName, statusQName, bean);
		
		// We run the commands in a temporary directory for this consumer
		// See Mark Basham as to why...
        runDir = File.createTempFile("MRC_", null);

		bean.setRunDirectory(runDir.getAbsolutePath());
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(runDir, "projectBean.json");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// If we have the -scriptLocation argument, use that
		scriptLocation = arguments.get("scriptLocation");
		if (scriptLocation==null || "null".equals(scriptLocation)) throw new IOException("-scriptLocation argument must be set");
		
		momlLocation = arguments.get("momlLocation");
		if (momlLocation==null || "null".equals(momlLocation)) throw new IOException("-momlLocation argument must be set");

	}

	@Override
	protected void execute() throws Exception {
		
		bean.setStatus(Status.RUNNING);
		bean.setPercentComplete(1);
		broadcast(bean);

        runPipeline();
	}

	/**
	 * Runs the pipeline on the cluster using qsub.
	 * @throws Exception
	 */
	protected void runPipeline() throws Exception {
		
		ProcessBuilder pb = new ProcessBuilder();

		// Can adjust env if needed:
		// Map<String, String> env = pb.environment();
		pb.directory(runDir);

		File log = new File(runDir, "mrc_output.txt");
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));

		final String cmd = createMRCCommand((FolderEventBean)bean);
		if (isWindowsOS()) {
			pb.command("cmd", "/C", cmd);
		} else {
			pb.command("bash", "-c", cmd);
		}

		System.out.println("Executing MRC pipeline in '"+runDir.getAbsolutePath()+"'");
		this.process = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert process.getInputStream().read() == -1;	

		// Now we check if xia2 itself failed
		// In order to know this we look for a file with the extension .error with a 
		// String in it "Error:"
		// We assume that this failure happens fast during this sleep.
		Thread.sleep(1000);
		checkMRCErrors();

		// Now we monitor the output file. Then we wait for the process, then we check for errors again.
		startProgressMonitor();
		createTerminateListener();
		process.waitFor();
		checkMRCErrors();						

		if (!bean.getStatus().isFinal()) {
			bean.setStatus(Status.COMPLETE);
			bean.setMessage("MRC pipeline completed normally");
			bean.setPercentComplete(100);
			broadcast(bean);
		}

	}
	
	private String createMRCCommand(FolderEventBean bean) {
		
		final StringBuilder buf = new StringBuilder();
		buf.append(scriptLocation);
		buf.append(" ");
		buf.append(momlLocation);
		
		if (bean.getProperties()!=null) {
			for(Object name : bean.getProperties().keySet()) {
				buf.append(" ");
				buf.append("-D");
				buf.append(name);
				buf.append("=");
				buf.append(bean.getProperties().get(name));
			}
		}
		return buf.toString();
	}
	

	private void checkMRCErrors() throws Exception {
		// TODO Auto-generated method stub
		
	}

	private void startProgressMonitor() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void terminate() throws Exception {
		
	    final int pid = getPid(process);
	    
	    System.out.println("killing pid "+pid);
	    // Not sure if this works
	    POSIX.INSTANCE.kill(pid, 9);
	}


}
