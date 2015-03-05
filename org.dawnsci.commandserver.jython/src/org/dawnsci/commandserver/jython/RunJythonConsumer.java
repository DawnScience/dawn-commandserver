package org.dawnsci.commandserver.jython;

import java.io.File;
import java.net.URI;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.dawnsci.commandserver.core.consumer.RemoteSubmission;

public class RunJythonConsumer {
	
	private static URI commandServerUri;
	private static JythonBean jBean;
	

	public static void main(String[] args) {
		
		//Set-up the options/arguments
		Options options = new Options();
		options.addOption("n", "name", true, "Set the name of the job to run.");
		options.addOption("m", "message", false, "A description of what the job will do.");
		
		//Parse the arguments
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try{
			cmd = parser.parse(options, args);
		} catch (Exception e) {
			System.out.println("Could not read the given options!\n"+e);
		}
		if (cmd == null) { //This is probably superfluous
			System.out.println("No parsed options available... cannot continue!");
			System.exit(1);
		}
		
		//Get the specified options
		String jobName = cmd.getOptionValue("n"); 
		String jobMessage = cmd.getOptionValue("m", "Jython consumer job submitted from the commandline.");
		String jobRunDir = System.getProperty("user.dir");
		
		//Get the arguments (should just be a script and its arguments - check this)
		String[] theArgs = cmd.getArgs();
		if (theArgs.length != 1) {
			System.out.println("Too many arguments given. Did you surround the argument with \"?");
			System.exit(1);
		}
		String[] argParts = theArgs[0].split(" ");
		File scriptFile = new File(argParts[0]);
		if (!scriptFile.exists() || scriptFile.isDirectory()) {
			System.out.println("Cannot find the script file. Did you give the full path?");
			System.exit(1);
		}
		String jobScript = theArgs[0];
		
		//Set up the job itself
		jBean = new JythonBean();
		jBean.setName(jobName);
		jBean.setMessage(jobMessage);
		jBean.setRunDirectory(jobRunDir);
		jBean.setJythonCode(jobScript);
		
		//Finally set up the submission system
		try{ 
			commandServerUri = new URI("tcp://sci-serv5.diamond.ac.uk:61616");
			final RemoteSubmission queueSub = new RemoteSubmission(commandServerUri);
			queueSub.setQueueName("scisoft.jython.SUBMISSION_QUEUE");
			queueSub.submit(jBean, true);
		} catch (Exception e) {
			System.out.println("Failed to start connect to the submission system!\n"+e);
		}
	}

}
