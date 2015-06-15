package org.dawnsci.commandserver.core.application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Start a consumer based on a file which contains the correct properties
 * to start a consumer using Consumer.create(...)
 * 
 * This class will create a separate process which can be started 
 * 
 * @author fcp94556
 *
 */
public class ConsumerProcess {
	
	private static final Logger logger = LoggerFactory.getLogger(ConsumerProcess.class);
	
	private static final String[] ESSENTIAL_PROPERTIES = new String[]{
		"uri","bundle","consumer","topic","status", "execLocation", "consumerName"
	};

	private File               propertiesFile;
	private Map<String,String> conf;
	private Process            process;


	public ConsumerProcess(File propertiesFile) throws Exception {
		this(Consumer.loadProperties(new HashMap<String, String>(7), propertiesFile.getAbsolutePath()));
		this.propertiesFile = propertiesFile;
	}
	
	/**
	 * Same arguments as Consumer.start but you must set 'execLocation'
	 * so that the path to the DAWN executable is known.
	 * @param conf
	 * @throws Exception
	 */
	public ConsumerProcess(Map<String,String> conf) throws Exception {
		this.conf = conf;
		for (String param : ESSENTIAL_PROPERTIES) {
			if (!conf.containsKey(param)) throw new Exception("Please set the parameter '"+param+"'");
		}
	}
	
	public Process start() throws Exception {
		
		
		final String line = createExecutionLine(conf);
		logger.debug("Execution line: "+line);
		final String[]  command;
		if (isLinuxOS()) {
			command = new String[]{"/bin/sh", "-c", line};
		} else {
			command = new String[]{"cmd", "/C", line};
		}
		 
		final Map<String,String> env = System.getenv();
		this.process     = Runtime.getRuntime().exec(command, getStringArray(env));
		
		String consumerName = conf.get("consumerName");
		if (consumerName==null) throw new Exception("You must set 'consumerName' because this is used for the workspace.");
		final String workspace = System.getProperty("user.home")+"/"+consumerName.replace(' ', '_');
		
		final File dir = new File(workspace);
		dir.mkdirs();

		PrintWriter   outs         = new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, "consumer_out.log"))));
		outs.write("Execution line: "+line);
		StreamGobbler out          = new StreamGobbler(process.getInputStream(), outs, "consumer output");
		out.setStreamLogsToLogging(true);
		out.start();
		
		PrintWriter   errs     = new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, "consumer_err.log"))));
		StreamGobbler err      = new StreamGobbler(process.getErrorStream(), errs, "consumer error");
		err.setStreamLogsToLogging(true);
		err.start();

		return process;
	}
	
	private String[] getStringArray(Map<String, String> env) {
		
		final String[] ret = new String[env.size()];
		int i = 0;
		for (String key : env.keySet()) {
			ret[i] = key+"="+env.get(key);
			++i;
		}
		return ret;
	}

	/*
	 * "$DAWN_RELEASE_DIRECTORY/dawn -noExit -noSplash -application org.dawnsci.commandserver.consumer -data ~/command_server 
	 * 
	 */
	private String createExecutionLine(Map<String, String> conf) throws Exception {
		
		final StringBuilder buf = new StringBuilder();
		
		// Get the path to the workspace and the model path
		String install   = isLinuxOS() ? conf.get("execLocation") : conf.get("winExecLocation");
		if (install == null) install = conf.get("execLocation");
		if (install == null) throw new Exception("'execLocation' must be set to the path to a DAWN executable!");
		
		String consumerName = conf.get("consumerName");
		if (consumerName==null) throw new Exception("You must set 'consumerName' because this is used for the workspace.");
		final String workspace = System.getProperty("user.home")+"/"+consumerName.replace(' ', '_');
		
		buf.append(install);
		buf.append(" -noExit -noSplash -application org.dawnsci.commandserver.consumer ");
		buf.append(" -data ");
		buf.append(workspace);
		buf.append(" -vmargs ");
		
		if (propertiesFile!=null) {
			buf.append("-Dproperties=");
			buf.append("\"");
			buf.append(propertiesFile.getAbsolutePath());
			buf.append("\"");
			
		} else {
			for(Object name : conf.keySet()) {
				buf.append(" ");
				buf.append("-D");
				buf.append(name);
				buf.append("=");
				String value = conf.get(name).toString().trim();
				if (value.contains(" ")) buf.append("\"");
				buf.append(value);
				if (value.contains(" ")) buf.append("\"");
			}
		}
		
		if (conf.containsKey("logLocation")) {
			buf.append(" >> "+conf.get("logLocation"));
		} else {
			buf.append(" >> "+workspace+"/consumer.log");
		}

		return buf.toString();
	}
	
	/**
	 * @return true if linux
	 */
	public static boolean isLinuxOS() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("linux");
	}

}
