package org.dawnsci.commandserver.mrc.consumer;

import java.io.File;
import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.core.process.ProgressableProcess;
import org.dawnsci.commandserver.core.producer.ProcessConsumer;
import org.dawnsci.commandserver.foldermonitor.EventType;
import org.dawnsci.commandserver.foldermonitor.FolderEventBean;

public class MRCConsumer extends ProcessConsumer {

	private BlockingQueue<ProgressableProcess> blockingProcesses;
	
	public MRCConsumer() {
		
		blockingProcesses = new LinkedBlockingQueue<ProgressableProcess>();
		final Thread runner = new Thread(new Runnable() {
			public void run() {

				System.out.println("Creating blocking queue");
				try {
					while(isActive()) {

						ProgressableProcess process = blockingProcesses.take();
						process.execute();
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, "Blocking process runner.");
		runner.setDaemon(true);
		runner.setPriority(Thread.NORM_PRIORITY);
		runner.start();
	}
	
	@Override
	protected Class<? extends StatusBean> getBeanClass() {
		// TODO Currently we must have a FolderEventBean to start this process
		// In future events might be triggered from GDA, we might need a generic
		// bean.
		return FolderEventBean.class;
	}

	@Override
	protected boolean isHandled(StatusBean bean) {
		
		FolderEventBean feb = (FolderEventBean)bean;
		if (feb.getType()!=EventType.ENTRY_CREATE) return false; // Only interested in new files.
		return true;
	}
	
	@Override
	protected ProgressableProcess createProcess(URI uri, String statusTName, String statusQName, StatusBean bean) throws Exception {
		
		// We are only interested in new files
		FolderEventBean feb = (FolderEventBean)bean;
		if (feb.getType()!=EventType.ENTRY_CREATE) return null; // Only interested in new files.
		
		feb.setName((new File(feb.getPath())).getName());
		
		MRCProcess process = new MRCProcess(uri, statusTName, statusQName,config, bean);
		if (config.containsKey("blocking")) {
			process.setBlocking(Boolean.parseBoolean(config.get("blocking")));
		} else {
			process.setBlocking(true); // One at a time
		}
		
		if (process.isBlocking()) {
			blockingProcesses.add(process);
			return null; // We have our own queue, this allows processes to show up in the UI.
		} else {
			return process;
		}
		
	}


	@Override
	public String getName() {
		return "EM Pipeline";
	}

	
	@Override
	protected long getMaximumRunningAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumXia2RunningAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumXia2RunningAge"));
		}
		return TWO_DAYS;
	}
		
	@Override
	protected long getMaximumCompleteAge() {
		if (System.getProperty("org.dawnsci.commandserver.core.maximumXia2CompleteAge")!=null) {
			return Long.parseLong(System.getProperty("org.dawnsci.commandserver.core.maximumXia2CompleteAge"));
		}
		return A_WEEK;
	}

}
