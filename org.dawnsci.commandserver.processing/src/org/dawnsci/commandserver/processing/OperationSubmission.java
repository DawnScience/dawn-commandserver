package org.dawnsci.commandserver.processing;

import java.io.File;
import java.net.URI;

import org.dawnsci.commandserver.core.consumer.RemoteSubmission;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;

/**
 * This class submits an IOperationContext for remote execution.
 * 
 * Might need to move it somewhere where anything can easily call it at some point.
 * 
 * <usage><code>
 * IOperationService oservice = ...
 * IOperationContext context  = oservice.createContext();
 * context.setData(...);
 * context.setSlicing(...);
 * context.setSeries(...);
 * 
 * OperationSubmission remote = new OperationSubmission(...);
 * remote.submit(context);
 * </code></usage>
 *
 * 
 * @author fcp94556
 *
 */
public class OperationSubmission extends RemoteSubmission {
	
	private static IPersistenceService  pservice;

	// Set by OSGI
	public static void setPersistenceService(IPersistenceService s) {
		pservice = s;
	}

	private String runDirectory;
	private String name;
	
	public OperationSubmission() {
		this(null);
	}
	
	public OperationSubmission(URI uri) {
		this(uri, getDefaultDirectory());
	}

	/**
	 * 
	 * @param uri
	 * @param sharedDirectory the place where the persistence file may be written that both consumer
	 *        and client can see.
	 */
	public OperationSubmission(URI uri, String sharedDirectory) {
		super(uri);
		setQueueName("scisoft.operation.SUBMISSION_QUEUE");
		runDirectory = sharedDirectory;
		name = "Operation pipeline";
	}

	/**
	 * Runs the operation context remotely on the consumer
	 * 
	 * @param context
	 * @return
	 * @throws Exception, for instance if connection cannot be made
	 */
	public OperationBean submit(IOperationContext context) throws Exception {
		
		OperationBean obean = prepare(context);
		super.submit(obean, true);
		return obean;
	}
	
	/**
	 * Use if you want to override fields in the bean before submitting it.
	 * Then submit with:
	 * <code> OperationSubmission.submit(obean, true); </code>
	 * 
	 * NOTE: This class writes the persistence file into the runDirectory
	 * 
	 * @param context
	 * @return
	 */
	public OperationBean prepare(IOperationContext context) throws Exception {

		if (context.getFilePath()==null) throw new Exception("You must set file path and dataset path with Remote running!");

		final OperationBean obean = new OperationBean();
		
		// StatusBean stuff
		obean.setRunDirectory(runDirectory);
		obean.setName(name);
		
		// Data stuff
		obean.setSlicing(context.getSlicing());
		obean.setFilePath(context.getFilePath());
		obean.setDatasetPath(context.getDatasetPath());
		
		// Series stuff
		final File persFile = new File(runDirectory+"/pipeline.nxs");
		persFile.getParentFile().mkdirs();
		if (persFile.exists()) persFile.delete();
		
		IPersistentFile file = pservice.createPersistentFile(persFile.getAbsolutePath());
		try {
		    file.setOperations(context.getSeries());
		    obean.setPersistencePath(persFile.getAbsolutePath());
		} finally {
			file.close();
		}
		
		// Run Stuff
		obean.setExecutionType(context.getExecutionType());
		obean.setParallelTimeout(context.getParallelTimeout());

		return obean;
	}

	public String getRunDirectory() {
		return runDirectory;
	}

	public void setRunDirectory(String runDirectory) {
		this.runDirectory = runDirectory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
	private static String getDefaultDirectory() {
		// Please override this to avoid 
		if (System.getProperty("os.name").toLowerCase().contains(("windows"))) {
			return "C:/tmp/operationPipeline";
		} else {
			return "/scratch/operationPipelineTest";
		}
	}

}
