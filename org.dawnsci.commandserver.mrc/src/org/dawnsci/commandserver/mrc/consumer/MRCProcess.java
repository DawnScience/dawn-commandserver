package org.dawnsci.commandserver.mrc.consumer;

import java.io.File;
import java.io.FileNotFoundException;
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

	private File    tmpDir;
	private String  scriptLocation;
	private String  momlLocation;
	private Process process;

	public MRCProcess(URI                uri, 
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
		tmpDir = getUnique(new File(visitDir, "tmp"), "emprocessing_", null, 1);
 		bean.setRunDirectory(tmpDir.getAbsolutePath());
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(tmpDir, "emBean.json");
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
		pb.directory(tmpDir);

		File log = new File(tmpDir, "em_output.txt");
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));

		final String cmd = createMRCCommand((FolderEventBean)bean);
		if (isWindowsOS()) {
			pb.command("cmd", "/C", cmd);
		} else {
			pb.command("bash", "-c", cmd);
		}

		System.out.println("Executing EM pipeline in '"+tmpDir.getAbsolutePath()+"'");
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
			bean.setMessage("EM pipeline completed normally");
			bean.setPercentComplete(100);
			broadcast(bean);
		}

	}
	
	private String createMRCCommand(FolderEventBean bean) throws Exception {
		
		final StringBuilder buf = new StringBuilder();
		buf.append(scriptLocation);
		buf.append(" ");
		buf.append(momlLocation);

		// We send visitdir as filepath and file name without extension as filename.
		final File newFile = new File(bean.getPath());
		if (!newFile.exists()) throw new FileNotFoundException("Cannot find "+bean.getPath());
		
		final File visitDir = newFile.getParentFile().getParentFile();
		buf.append(" ");
		buf.append("-Dfilepath"); // should be called visitDir?
		buf.append("=");
		buf.append(visitDir.getAbsolutePath());
		
		final String fileName = getFileNameNoExtension(newFile);
		buf.append(" ");
		buf.append("-Dfileroot"); // should be called fileNameNoExtension?
		buf.append("=");
		buf.append(fileName);
		
		if (bean.getProperties()!=null) {
			for(Object name : bean.getProperties().keySet()) {
				buf.append(" ");
				buf.append("-D");
				buf.append(name);
				buf.append("=");
				String value = bean.getProperties().get(name).toString().trim();
				if (value.contains(" ")) value = value.replace(" ", "\\ ");
				buf.append(value);
			}
		}
		return buf.toString();
	}
	
	public static String getFileNameNoExtension(File file) {
		return getFileNameNoExtension(file.getName());
	}

	/**
	 * Get Filename minus it's extension if present
	 * 
	 * @param file
	 *            File to get filename from
	 * @return String filename minus its extension
	 */
	public static String getFileNameNoExtension(String fileName) {
		int posExt = fileName.lastIndexOf(".");
		// No File Extension
		return posExt == -1 ? fileName : fileName.substring(0, posExt);

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
