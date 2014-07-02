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
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * A process which blocks until done.
 * @author fcp94556
 *
 */
public class MRCProcess extends ProgressableProcess {

	private File    tmpDir;
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
		momlLocation = arguments.get("momlLocation");
		if (momlLocation==null || "null".equals(momlLocation)) throw new IOException("-momlLocation argument must be set");

		bean.setStatus(Status.QUEUED);
		bean.setPercentComplete(0);
		broadcast(bean);

	}

	@Override
	public void execute() throws Exception {
		
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
		buf.append(createModuleLoadCommand());
		buf.append(" ");
		buf.append(createWorkflowCommand(momlLocation));

		// We send visitdir as filepath and file name without extension as filename.
		final File newFile = new File(bean.getPath());
		if (!newFile.exists()) throw new FileNotFoundException("Cannot find "+bean.getPath());
		
		final File visitDir = newFile.getParentFile().getParentFile();
		buf.append(" ");
		buf.append("-Dfilepath"); // should be called visitDir?
		buf.append("=");
		final String visitDirPath = visitDir.getAbsolutePath();
		if (visitDirPath.contains(" ")) buf.append("\"");
		buf.append(visitDirPath);
		if (visitDirPath.contains(" ")) buf.append("\"");
		
		final String fileName = getFileNameNoExtension(newFile);
		buf.append(" ");
		buf.append("-Dfileroot"); // should be called fileNameNoExtension?
		buf.append("=");
		if (fileName.contains(" ")) buf.append("\"");
		buf.append(fileName);
		if (fileName.contains(" ")) buf.append("\"");
		
		if (bean.getProperties()!=null) {
			for(Object name : bean.getProperties().keySet()) {
				buf.append(" ");
				buf.append("-D");
				buf.append(name);
				buf.append("=");
				String value = bean.getProperties().get(name).toString().trim();
				if (value.contains(" ")) buf.append("\"");
				buf.append(value);
				if (value.contains(" ")) buf.append("\"");
			}
		}
		return buf.toString();
	}
	
	private String createWorkflowCommand(String momlLocation) {
		
		final StringBuilder buf = new StringBuilder();
		buf.append("$DAWN_RELEASE_DIRECTORY/dawn -noSplash -application com.isencia.passerelle.workbench.model.launch -data \"");
		final String location = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
		buf.append(location);
		buf.append("\" -consolelog -os linux -ws gtk -arch x86_64 -vmargs -Dorg.dawb.workbench.jmx.headless=true -Dcom.isencia.jmx.service.terminate=true -Dmodel=\"");
		buf.append(momlLocation);
		buf.append("\" ");
		return buf.toString();
	}

	private String createModuleLoadCommand() {
		
		// TODO System property?
		final StringBuilder buf = new StringBuilder();
		// TODO Way to set module load command...
		buf.append("module load dawn/snapshot");
		buf.append(" ; ");
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
	public void terminate() throws Exception {
		
	    final int pid = getPid(process);
	    
	    System.out.println("killing pid "+pid);
	    // Not sure if this works
	    POSIX.INSTANCE.kill(pid, 9);
	}


}
