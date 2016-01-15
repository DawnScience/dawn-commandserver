/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.foldermonitor;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

import org.dawnsci.commandserver.core.ActiveMQServiceHolder;
import org.dawnsci.commandserver.core.application.IConsumerExtension;
import org.eclipse.scanning.api.event.EventException;
import org.eclipse.scanning.api.event.IEventService;
import org.eclipse.scanning.api.event.alive.HeartbeatBean;
import org.eclipse.scanning.api.event.alive.KillBean;
import org.eclipse.scanning.api.event.bean.BeanEvent;
import org.eclipse.scanning.api.event.bean.IBeanListener;
import org.eclipse.scanning.api.event.core.IPublisher;
import org.eclipse.scanning.api.event.core.ISubscriber;
import org.eclipse.scanning.api.event.status.Status;
import org.eclipse.scanning.api.event.status.StatusBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which monitors a folder location and publishes a topic and
 * adds the event to queue. This class also appears in the consumer
 * list on the client as it populates alive events.
 * 
 * The command line parameter "location" must be set when starting the consumer.
 * 
 * @author Matthew Gerring
 *
 */
public class Monitor implements IConsumerExtension{
	
	private static final Logger logger = LoggerFactory.getLogger(Monitor.class);
	
	private boolean       stopped;
	private URI           uri;
	
	private Path          dir;
	private String        location;
	private Map<String, String> config;
	private Pattern       filePattern;

	private WatchService watcher;
	
	private IPublisher<StatusBean> broadcaster;

	@Override
	public void init(Map<String, String> configuration) throws Exception {
		
		this.config = configuration;
		setUri(new URI(configuration.get("uri")));
		String queue = configuration.get("status");
		String topic = configuration.get("topic");
		
		IEventService service = ActiveMQServiceHolder.getEventService();
		this.broadcaster   = service.createPublisher(getUri(), topic);
		broadcaster.setQueueName(queue);
		System.out.println("Folder monitor topic is '"+broadcaster.getTopicName()+"'");
		System.out.println("Folder monitor queue is '"+broadcaster.getQueueName()+"'");


		this.location = configuration.get("location");
		final File   fdir     = new File(configuration.get("location"));
		if (!fdir.exists()) throw new Exception(location+" does not exist and cannot be monitored!");

		this.dir       = Paths.get(location);
		
	}

	private ISubscriber<IBeanListener<KillBean>> killer;
	private IPublisher<HeartbeatBean>            alive;
	
	@Override
	public void start() throws Exception {
		
		
		final UUID consumerId = UUID.randomUUID();
		
		IEventService service = ActiveMQServiceHolder.getEventService();
		
		this.alive  = service.createPublisher(uri, IEventService.HEARTBEAT_TOPIC,  null);
		alive.setConsumerId(consumerId);
		alive.setConsumerName(getName());

		this.killer = service.createSubscriber(uri, IEventService.KILL_TOPIC, null);
		killer.addListener(new IBeanListener<KillBean>() {

			@Override
			public void beanChangePerformed(BeanEvent<KillBean> evt) {
				KillBean kbean = evt.getBean();
				if (kbean.getConsumerId().equals(consumerId)) {
					try {
						stop();
						if (kbean.isDisconnect()) disconnect();
					} catch (EventException e) {
						logger.error("An internal error occurred trying to terminate the consumer "+getName()+" "+consumerId);
					}
					if (kbean.isExitProcess()) {
						try {
							Thread.sleep(2500);
						} catch (InterruptedException e) {
							logger.error("Unable to pause before exit", e);
						}
						System.exit(0); // Normal orderly exit
					}
				}
			}
		});

		startMonitor();
	}
	
	private void startMonitor() throws Exception {
		
        if (config.containsKey("filePattern")) {
        	filePattern = Pattern.compile(config.get("filePattern")); // Might throw exception.
        	System.out.println("File name matching set to '"+filePattern+"'");
        }

		boolean nio = Boolean.parseBoolean(config.get("nio"));
		if (nio) {
            startNio();
		} else {
			boolean recursive = Boolean.parseBoolean(config.get("recursive"));
            if (recursive) throw new IllegalArgumentException("Cannot use recursive monitoring with nio!");
			
            startPolling();
		}
	}

	private void startPolling() throws Exception {
		
		final long sleepTime = config.get("sleepTime") != null ? Long.parseLong(config.get("sleepTime")) : 1000L;
		System.out.println("Starting polling folder monitor @ '"+dir+"' with sleepTime of "+sleepTime+" ms");

		// We initiate the file list and last modified times.
		Map<Path, FileTime> fileList = readFileList(dir);
		
		while(!stopped) {
			
			Thread.sleep(sleepTime); // Can be interrupted
			
			try {
				Map<Path, FileTime> currentList = readFileList(dir);
				
				if (currentList.equals(fileList)) continue;
				
				// Otherwise we notify of new, modified and deleted files
				
				Map<Path, FileTime> tmp = 	new HashMap<Path, FileTime>(fileList);
			    tmp.keySet().removeAll(currentList.keySet());
			    if (tmp.size()>0)  broadcast(tmp, EventType.ENTRY_DELETE);
	
			    tmp = 	new HashMap<Path, FileTime>(currentList);
			    tmp.keySet().removeAll(fileList.keySet());		    
			    if (tmp.size()>0)  broadcast(tmp, EventType.ENTRY_CREATE);
			    		    
			    for (Path path : currentList.keySet()) {
					if (fileList.containsKey(path)) {
						final FileTime oldTime = fileList.get(path);
						final FileTime newTime = currentList.get(path);
						
						if (!oldTime.equals(newTime)) {
			                System.out.format("%s: %s\n", ENTRY_MODIFY, path);
							broadcaster.broadcast(bean(ENTRY_MODIFY, path));
						}
					}
				}
			    
			    fileList = currentList;
			} catch (java.nio.file.NoSuchFileException nfe) {
				// Files can be deleted and not walked.
				continue;
			}
		}
	}
	
	private void broadcast(Map<Path, FileTime> fileList, EventType type) throws Exception {
		
	    for (Path path : fileList.keySet()) {
            System.out.format("%s: %s\n", type, path);
		    broadcaster.broadcast(bean(type, path));
	    }
	}

	private Map<Path, FileTime> readFileList(final Path dir) throws IOException {
		
		final Map<Path, FileTime> ret = new HashMap<Path, FileTime>(31);
		
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>()  {
            @Override
            public FileVisitResult preVisitDirectory(Path file, BasicFileAttributes attrs) throws IOException {
            	if (file.equals(dir)) return FileVisitResult.CONTINUE;
            	return FileVisitResult.SKIP_SUBTREE;
            }
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (filePattern!=null) {
                	if (!filePattern.matcher(file.getFileName().toString()).matches()) {
                		return FileVisitResult.CONTINUE;
                	}
                }
            	ret.put(file, attrs.lastModifiedTime());
            	return FileVisitResult.CONTINUE;
            }       
        });

		return ret;
	}

	private void startNio() throws Exception {
		
		this.watcher   = FileSystems.getDefault().newWatchService();
		
		boolean recursive = Boolean.parseBoolean(config.get("recursive"));
		if (recursive) {
			registerAll(dir);
		} else {
			dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		}

		
		System.out.println("Starting nio folder monitor @ '"+dir+"'. Recursive is "+(recursive?"on":"off"));
		while(!stopped) {
			
			WatchKey key = watcher.take();
			
			List<WatchEvent<?>> events = key.pollEvents();
			for (WatchEvent<?> event : events) {

				Kind kind = event.kind();
				if (kind == OVERFLOW)  continue;

				WatchEvent<?> ev = (WatchEvent<?>)event;
				Object   context = ev.context();
				if (!(context instanceof Path)) continue;

				Path name  = (Path)context;
				Path child = dir.resolve(name);
				try {

					if (filePattern!=null) {
						if (!filePattern.matcher(child.getFileName().toString()).matches()) {
							continue;
						}
					}

					// print out event
					System.out.format("%s: %s\n", kind, child);
					StatusBean bean = bean(kind, child);

					broadcaster.broadcast(bean);

				} finally {

	                if (recursive && (kind == ENTRY_CREATE)) {
	                	if (Files.isDirectory(child)) {
	                		registerAll(child);
	                	}
	                }
				}
                
			}
			
			boolean valid = key.reset();
		    if (!valid) throw new Exception("Cannot monitor '"+key+"'");
		}
		System.out.println("Finished folder monitor @ '"+dir+"'");
	}

	private StatusBean bean(Kind kind, Path child) throws IOException {
		return bean(EventType.valueOf(kind.name()), child);
	}
	
	private StatusBean bean(EventType type, Path child) throws IOException {
		
		StatusBean bean = new StatusBean();
		bean.setStatus(Status.NONE);
		try {
			bean.setHostName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			// Not fatal but would be nice...
			e.printStackTrace();
		}
		bean.setUniqueId(System.currentTimeMillis()+"_"+UUID.randomUUID());
		bean.setRunDirectory(child.getParent().toAbsolutePath().toString());
		bean.setName(child.getFileName().toString());
		bean.setSubmissionTime(System.currentTimeMillis());
		
		if (config.get("extraProperties")!=null) {
			bean.setProperties(loadProperties(config.get("extraProperties")));
		}
		
		final String path = child.toAbsolutePath().toString();
		
		final File newFile = new File(path);
		bean.setProperty("file_name",  newFile.getName());
		bean.setProperty("file_path",  path);
		bean.setProperty("file_dir",   newFile.getParent());
		bean.setProperty("event_type", type.name());
		bean.setProperty("is_file",    String.valueOf(newFile.isFile()));
		
		if (newFile.exists()) {
		
			final File visitDir = newFile.getParentFile().getParentFile();
	
			String visitDirPath = visitDir.getAbsolutePath();
			if (visitDirPath.contains(" ")) visitDirPath = "\""+visitDirPath+"\"";
			bean.setProperty("filepath",visitDirPath);
			
			String fileName = getFileNameNoExtension(newFile);
			if (fileName.contains(" ")) fileName = "\""+fileName+"\"";
			bean.setProperty("fileroot",fileName);
		}
		
		return bean;
	}
	
	private final static Properties loadProperties(final String path) throws IOException {
		return loadProperties(new File(path));
	}
	private final static Properties loadProperties(final File file) throws IOException {   	
		if (!file.exists()) return new Properties();
		return loadProperties(new FileInputStream(file));
	}

	public final static Properties loadProperties(final InputStream stream) throws IOException {   	
		
		final Properties fileProps       = new Properties();
		try {
			final BufferedInputStream in = new BufferedInputStream(stream);
			fileProps.load(in);
		} finally {
			stream.close();
		}
		return fileProps;
	}


	@Override
	public void stop() throws EventException {
		
		try {
			System.out.println("Stopping folder monitor @ '"+dir+"'");
			if (watcher!=null) watcher.close();
			stopped = true;
			disconnect();
			Thread.sleep(2000);
		} catch (EventException ne) {
			throw ne;
		} catch (Exception e) {
			throw new EventException("Cannot stop consumer "+getName(), e);
		}
	}

 
    private void disconnect() throws EventException {
    	broadcaster.disconnect();
    	alive.disconnect();
    	killer.disconnect();
    }

	/**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                return FileVisitResult.CONTINUE;
            }
        });
    }

	@Override
	public String getName() {
		if (config.containsKey("consumerName")) {
			return config.get("consumerName")+"'"+location+"'";
		}
		return "Folder monitoring '"+location+"'";
	}
	
	private static String getFileNameNoExtension(File file) {
		return getFileNameNoExtension(file.getName());
	}

	/**
	 * Get Filename minus it's extension if present
	 * 
	 * @param file
	 *            File to get filename from
	 * @return String filename minus its extension
	 */
	private static String getFileNameNoExtension(String fileName) {
		int posExt = fileName.lastIndexOf(".");
		// No File Extension
		return posExt == -1 ? fileName : fileName.substring(0, posExt);

	}

    /**
     * Used by tests to override data and start a monitor
     * @param conf
     */
	public void mock(Map<String,String> conf, Path dir) {
		this.config      = conf;
		this.dir         = dir;
		
		final Thread monitorThread = new Thread(new Runnable() {
			public void run() {
				try {
					startMonitor();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, "Mock File Monitor Thread");
		monitorThread.setDaemon(true);
		monitorThread.start();
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}

	public void setBroadcaster(IPublisher<StatusBean> b) {
		this.broadcaster = b;
	}
}
