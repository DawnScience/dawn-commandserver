package org.dawnsci.commandserver.mx.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.mx.beans.ProjectBean;

/**
 * Rerun of several collections as follows:
 * o Write the Xia2 command file, automatic.xinfo
 * o Runs Xia2 with file
 * o Progress reported by stating xia2.txt
 * o Runs xia2 html to generate report.
 * 
 * @author fcp94556
 *
 */
public class Xia2Process extends ProgressableProcess{
	
	private static String SETUP_COMMAND = "module load xia2"; // Change by setting org.dawnsci.commandserver.mx.moduleCommand
	private static String XIA2_COMMAND  = "xia2 -xinfo automatic.xinfo"; // Change by setting org.dawnsci.commandserver.mx.xia2Command

	private String processingDir;
	
	public Xia2Process(String     uri, 
			           String     statusTName, 
			           String     statusQName,
			           ProjectBean bean) {
		super(uri, statusTName, statusQName, bean);
		
        final String runDir  = bean.getRunDirectory();
		final File   xia2Dir = getUnique(new File(runDir), "MultiCrystal_", null, 1);
		xia2Dir.mkdirs();
		
		// Example:
		//   /dls/i03/data/2014/cm4950-2/2014-04-10/processing/fake161118/MultiCrystal_2
		
		processingDir = xia2Dir.getAbsolutePath();
		bean.setRunDirectory(processingDir);
		
	}

	@Override
	public void run() {
		
		writeFile();
		runXia2();
	}

	private void runXia2() {
		
		ProcessBuilder pb = new ProcessBuilder();
		
		// Can adjust env if needed:
		// Map<String, String> env = pb.environment();
		pb.directory(new File(processingDir));
		
		File log = new File(processingDir, "xia2_output.txt");
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));
		
		pb.command("bash", "-c", createXai2Command());
		
		try {
			Process p = pb.start();
			assert pb.redirectInput() == Redirect.PIPE;
			assert p.getInputStream().read() == -1;		
		} catch (Exception ne) {
			bean.setStatus(Status.FAILED);
			bean.setMessage(ne.getMessage());
			bean.setPercentComplete(0);
			broadcast(bean);
		}		

	}
	
	private String createXai2Command() {
		
		// Get a linux enviroment		
		String setupCmd = System.getProperty("org.dawnsci.commandserver.mx.moduleCommand")!=null
				        ? System.getProperty("org.dawnsci.commandserver.mx.moduleCommand")
				        : SETUP_COMMAND;

		String xia2Cmd = System.getProperty("org.dawnsci.commandserver.mx.xia2Command")!=null
	               ? System.getProperty("org.dawnsci.commandserver.mx.xia2Command")
	               : XIA2_COMMAND;
	               
	    return setupCmd+" ; "+xia2Cmd;
	}

	private void writeFile() {
        ProjectBean dBean = (ProjectBean)bean;
        Xia2Writer writer = null;
		try {
	        
	        final File dir = new File(processingDir);
	        dir.mkdirs();
			
	        writer = new Xia2Writer(new File(dir, Xia2Writer.DEFAULT_FILENAME));
	        writer.write(dBean);
	        
			dBean.setStatus(Status.RUNNING);
			dBean.setPercentComplete(1);
			broadcast(dBean);

	        
		} catch (Exception ne) {
			dBean.setStatus(Status.FAILED);
			dBean.setMessage(ne.getMessage());
			broadcast(dBean);
			
		} finally {
			if (writer!=null) {
				try {
					writer.close();
				} catch (IOException e) {
					dBean.setStatus(Status.FAILED);
					dBean.setMessage(e.getMessage());
					broadcast(dBean);
				}
			}
		}
		
	}

	public String getProcessingDir() {
		return processingDir;
	}

	public void setProcessingDir(String processingDir) {
		this.processingDir = processingDir;
	}


	/**
	 * @param dir
	 * @param template
	 * @param ext
	 * @param i
	 * @return file
	 */
	private static File getUnique(final File dir, final String template, final String ext, int i) {
		final String extension = ext != null ? (ext.startsWith(".")) ? ext : "." + ext : null;
		final File file = ext != null ? new File(dir, template + i + extension) : new File(dir, template + i);
		if (!file.exists()) {
			return file;
		}

		return getUnique(dir, template, ext, ++i);
	}

}
