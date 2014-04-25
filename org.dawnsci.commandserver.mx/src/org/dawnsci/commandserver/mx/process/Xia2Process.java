package org.dawnsci.commandserver.mx.process;

import java.io.File;
import java.io.IOException;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.beans.StatusBean;
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

	private String processingDir;
	
	public Xia2Process(String     uri, 
			           String     statusTName, 
			           String     statusQName,
			           StatusBean bean) {
		super(uri, statusTName, statusQName, bean);
		
		processingDir = "C:/tmp/xia2Testing"; // TODO Set this or work it out.
	}

	@Override
	public void run() {
		
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

}
