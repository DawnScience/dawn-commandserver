package org.dawnsci.commandserver.processing;

import java.io.File;
import java.net.URI;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.dawnsci.commandserver.processing.beans.OperationBean;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.IOperationContext;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.core.ISubmitter;

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
 * @author Matthew Gerring
 *
 */
public class OperationSubmission {
	
	private static IPersistenceService  pservice;

	// Set by OSGI
	public static void setPersistenceService(IPersistenceService s) {
		pservice = s;
	}
	
	private ISubmitter<OperationBean> submitter;


	private String runDirectory;
	private String name;
	private String queueName;


	private URI uri;
	
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
		
		this.uri = uri;
		this.queueName = "scisoft.operation.SUBMISSION_QUEUE";
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
		
		if (submitter==null) {
			IEventService service = ActiveMQServiceHolder.getEventService();
			this.submitter = service.createSubmitter(uri, queueName);
		}
		submitter.submit(obean, true);
		
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
		obean.setDataDimensions(context.getDataDimensions());
		obean.setFilePath(context.getFilePath());
		obean.setDatasetPath(context.getDatasetPath());
		
		// Series stuff
		final File persFile = getUnique(new File(runDirectory), "pipeline", "nxs", 1);
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

	public void prepare(OperationBean obean) throws Exception {

		// StatusBean stuff
		obean.setRunDirectory(runDirectory);
		obean.setName(name);
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

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) throws EventException {
		this.queueName = queueName;
		if (submitter!=null) submitter.setSubmitQueueName(queueName);
	}

	public URI getUri() {
		return uri;
	}

	public void directSubmit(OperationBean obean) throws EventException {
		if (submitter==null) {
			IEventService service = ActiveMQServiceHolder.getEventService();
			this.submitter = service.createSubmitter(uri, queueName);
		}
		submitter.submit(obean, true);
	}

}
