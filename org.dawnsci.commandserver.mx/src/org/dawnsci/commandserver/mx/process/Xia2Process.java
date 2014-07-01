package org.dawnsci.commandserver.mx.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.process.POSIX;
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
	
	private final static String SETUP_COMMAND = "module load xia2"; // Change by setting org.dawnsci.commandserver.mx.moduleCommand	
	private final static String XIA2_NAME     = "xia2";             // Change by setting org.dawnsci.commandserver.mx.xia2Command
	
	// Change by setting environment variable XIA2_FIXEDCMD
	private final static String XIA2_FIXEDCMD;
	static {
		String fixedCmd = System.getenv("XIA2_FIXEDCMD");
		if (fixedCmd!=null) {
			XIA2_FIXEDCMD = fixedCmd;
		} else {
			XIA2_FIXEDCMD = "-xparallel -1 -blend -ispyb_xml_out ispyb.xml -xinfo automatic.xinfo";
		}
	}
	
	private final static String XIA2_FILE     = "xia2.txt"; // Change by setting org.dawnsci.commandserver.mx.xia2Command

	private String processingDir;
	private String scriptLocation;
	private Process process;
	
	public Xia2Process(URI        uri, 
			           String     statusTName, 
			           String     statusQName,
			           Map<String,String> arguments,
			           ProjectBean bean) {
		
		super(uri, statusTName, statusQName, bean);
		
        final String runDir;
		if (isWindowsOS()) {
			// We are likely to be a test consumer, anyway the unix paths
			// from ISPyB will certainly not work, so we process in C:/tmp/
			runDir  = "C:/tmp/"+bean.getRunDirectory();
		} else {
			runDir  = bean.getRunDirectory();
		}

 		final File   xia2Dir = getUnique(new File(runDir), "MultiCrystal_", null, 1);
		xia2Dir.mkdirs();
		
		// Example:
		//   /dls/i03/data/2014/cm4950-2/20140425/gw/processing/thau1/MultiCrystal_12
		
	    processingDir = xia2Dir.getAbsolutePath();
		bean.setRunDirectory(processingDir);
		
		// We record the bean so that reruns of reruns are possible.
		try {
			writeProjectBean(processingDir, "projectBean.json");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// If we have the -scriptLocation argument, use that
		scriptLocation = "";
		if (arguments.containsKey("scriptLocation")) {
			scriptLocation = arguments.get("scriptLocation")+" ";
		}
 	}

	@Override
	public void execute() throws Exception {
		
		writeFile();
		
		bean.setStatus(Status.RUNNING);
		bean.setPercentComplete(1);
		broadcast(bean);
		
		runXia2();
	}

	/**
	 * Forcibly kills a process tree by default. You may override the terminate 
	 * for instance when a job should be killed on the cluster.
	 *  
	 * You must manually call createTerminateListener() or the terminate will not be 
	 * listened to and the topic will never trigger this method to be called.
	 *  
	 * @param p
	 * @throws Exception
	 */
	protected void terminate() throws Exception {

	    final int pid = getPid(process);
	    
	    System.out.println("killing pid "+pid);
	    // Not sure if this works
	    POSIX.INSTANCE.kill(pid, 9);
	    
	}


	private void runXia2() throws Exception {

		ProcessBuilder pb = new ProcessBuilder();

		// Can adjust env if needed:
		// Map<String, String> env = pb.environment();
		pb.directory(new File(processingDir));

		File log = new File(processingDir, "xia2_output.txt");
		pb.redirectErrorStream(true);
		pb.redirectOutput(Redirect.appendTo(log));

		if (isWindowsOS()) {
			pb.command("cmd", "/C", createXai2Command((ProjectBean)bean));
		} else {
			pb.command("bash", "-c", createXai2Command((ProjectBean)bean));
		}

		this.process = pb.start();
		assert pb.redirectInput() == Redirect.PIPE;
		assert process.getInputStream().read() == -1;	

		// Now we check if xia2 itself failed
		// In order to know this we look for a file with the extension .error with a 
		// String in it "Error:"
		// We assume that this failure happens fast during this sleep.
		Thread.sleep(1000);
		checkXia2Errors(); // We do this to avoid starting an output
		// file monitor at all.

		// Now we monitor the output file. Then we wait for the process, then we check for errors again.
		startProgressMonitor();
		createTerminateListener();
		process.waitFor();
		checkXia2Errors();						

		if (!bean.getStatus().isFinal()) {
			bean.setStatus(Status.COMPLETE);
			bean.setMessage("Xia2 run completed normally");
			bean.setPercentComplete(100);
			broadcast(bean);
		}

	}


    private static final Pattern STATUS_LINE = Pattern.compile("\\-+ Integrating ([a-zA-Z0-9_]+) \\-+");
	/**
     * Starts file polling on the output file, stops when bean reaches a final state.
     */
	private void startProgressMonitor() {
		
		final Thread poll = new Thread(new Runnable() {
			public void run() {
			
				try {
					final Set<String> processedSweeps = new HashSet<String>();
					
					// First wait until file is there or bean is done.
					final String name     = System.getProperty("org.dawnsci.commandserver.mx.xia2OutputFile")!=null
					                      ? System.getProperty("org.dawnsci.commandserver.mx.xia2OutputFile")
					                      : XIA2_FILE;
					                      
					final File xia2Output = new File(processingDir, name);
					while(!bean.getStatus().isFinal() && !xia2Output.exists()) {
	                    Thread.sleep(1000);
					}
					
					// Now the file must exist or we are done
					if (xia2Output.exists()) {
						BufferedReader br = null;
						try {
							br = new BufferedReader( new FileReader(xia2Output) );
						    while(!bean.getStatus().isFinal()) {
								String line = br.readLine();
								if (line==null) {
									Thread.sleep(2000); // Xia2 writes some more lines
									continue;
								}
								
								if (line.contains("No images assigned for crystal test")) {
									bean.setStatus(Status.FAILED);
									bean.setMessage(line);
									bean.setPercentComplete(0);
									broadcast(bean);
									return;
								}
								
								final Matcher matcher = STATUS_LINE.matcher(line);
								if (matcher.matches()) {
									final String sweepName = matcher.group(1);
									processedSweeps.add(sweepName); // They are not in order!
									
									ProjectBean pbean = (ProjectBean)bean;
									final double complete = (processedSweeps.size()/(double)pbean.getSweeps().size())*100d;
									System.out.println("XIA2 % commplete>> "+complete);
									
									bean.setMessage("Integrating "+sweepName);
									bean.setPercentComplete(complete);
									broadcast(bean);
									System.out.println("XIA2>> "+line);
									continue;
								}
								
								if (line.startsWith("--------------------")) {
									bean.setStatus(Status.RUNNING);
									bean.setMessage(line.substring("--------------------".length()));
									broadcast(bean);
								}
								
								// TODO parse the lines when we have them
								// broadcast any %-complete that we think we have found.
								System.out.println("XIA2>> "+line);
								
								
							} 
						} finally {
							if (br!=null) br.close();
						}
					}
					
				} catch (Exception ne) {
					// Should we send the failure to monitor the file as an error?
					// NOPE because xia2 writes an error file, that info is more useful.
					ne.printStackTrace();
				}
			}
		});
		poll.setName("xia2.txt polling thread");
		poll.setPriority(Thread.MIN_PRIORITY);
		poll.setDaemon(true);
		poll.start();
	}

	/**
	 * Throws an exception if an error file is found with the string Error: in it.
	 * @throws Exception
	 */
	private void checkXia2Errors() throws Exception {
		
		final File dir = new File(processingDir);
		for (File c : dir.listFiles()) {
			if (c.isFile() && c.getName().toLowerCase().endsWith(".error")) {
				checkErrorFile(c);
			}
			
			if (c.isFile() && c.getName().toLowerCase().contains("xia2_output.txt")) {
				checkNotRecognised(c);
			}
		}
	}

	private void checkNotRecognised(File c) throws Exception {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(c));
			
			String line = null;
			while((line = br.readLine())!=null) {
				if (line.contains("not recognized")) {
					throw new Exception(line);
				}
			}
		} finally {
			if (br!=null) br.close();
		}
		
	}

	private void checkErrorFile(File c) throws Exception {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(c));
			
			String line = null;
			while((line = br.readLine())!=null) {
				if (line.contains("Error:")) {
					final String[] split = line.split(":");
					throw new Exception(split[1]);
				}
			}
		} finally {
			if (br!=null) br.close();
		}
	}

	private String createXai2Command(ProjectBean bean) {
		
		String setupCmd = "";
		if (!isWindowsOS()) { // We use module load xia2
			// Get a linux enviroment		
			setupCmd = System.getProperty("org.dawnsci.commandserver.mx.moduleCommand")!=null
					        ? System.getProperty("org.dawnsci.commandserver.mx.moduleCommand")
					        : SETUP_COMMAND;
					        
			setupCmd+=" ; ";
		}

		// For windows xia2 must be on the path already.
		String xia2Cmd = System.getProperty("org.dawnsci.commandserver.mx.xia2Command");
		
		if (xia2Cmd==null) {
			String cmd = bean.getCommandLineSwitches();
			if (cmd==null) cmd = "";
			xia2Cmd = scriptLocation+XIA2_NAME+" "+cmd+" "+XIA2_FIXEDCMD;
		}
	               
	    return setupCmd+xia2Cmd;
	}

	private void writeFile() throws Exception {
		
        ProjectBean dBean = (ProjectBean)bean;
        Xia2Writer writer = null;
		try {
	        
	        final File dir = new File(processingDir);
	        dir.mkdirs();
			
	        writer = new Xia2Writer(new File(dir, Xia2Writer.DEFAULT_FILENAME));
	        writer.write(dBean);
	            
	        			
		} finally {
			if (writer!=null) writer.close();
		}
		
	}

	public String getProcessingDir() {
		return processingDir;
	}

	public void setProcessingDir(String processingDir) {
		this.processingDir = processingDir;
	}

}
