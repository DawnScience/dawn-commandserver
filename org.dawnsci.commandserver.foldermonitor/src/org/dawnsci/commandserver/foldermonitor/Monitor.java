package org.dawnsci.commandserver.foldermonitor;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.File;
import java.io.IOException;
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
import java.util.UUID;
import java.util.regex.Pattern;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.dawnsci.commandserver.core.ConnectionFactoryFacade;
import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.core.producer.AliveConsumer;
import org.dawnsci.commandserver.core.producer.Broadcaster;

/**
 * Class which monitors a folder location and publishes a topic and
 * adds the event to queue. This class also appears in the consumer
 * list on the client as it populates alive events.
 * 
 * The command line parameter "location" must be set when starting the consumer.
 * 
 * @author fcp94556
 *
 */
public class Monitor extends AliveConsumer {
	
	private boolean       stopped;
	
	private WatchService  watcher;
	private Path          dir;
	private Connection    connection;
	private String        location;
	private Broadcaster   broadcaster;
	private Map<String, String> config;
	private Pattern       filePattern;

	@Override
	public void init(Map<String, String> configuration) throws Exception {
		
		this.config = configuration;
		setUri(new URI(configuration.get("uri")));
		String queue = configuration.get("status");
		String topic = configuration.get("topic");
		
		this.broadcaster   = new Broadcaster(getUri(), queue, topic);
		System.out.println("Folder monitor topic is '"+broadcaster.getTopicName()+"'");
		System.out.println("Folder monitor queue is '"+broadcaster.getQueueName()+"'");

		ConnectionFactory connectionFactory = ConnectionFactoryFacade.createConnectionFactory(getUri());		
		connection = connectionFactory.createConnection();
		connection.start();

		this.location = configuration.get("location");
		final File   fdir     = new File(configuration.get("location"));
		if (!fdir.exists()) throw new Exception(location+" does not exist and cannot be monitored!");

		this.dir       = Paths.get(location);
		this.watcher   = FileSystems.getDefault().newWatchService();
		
		boolean recursive = Boolean.parseBoolean(configuration.get("recursive"));
		if (recursive) {
			registerAll(dir);
		} else {
			dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		}
		
        if (configuration.containsKey("filePattern")) {
        	filePattern = Pattern.compile(configuration.get("filePattern")); // Might throw exception.
        	System.out.println("File name matching set to '"+filePattern+"'");
        }
	}
	
	@Override
	public void start() throws Exception {
		
		startNotifications(); // Tell the GUI that we are alive
		
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
		
		boolean recursive = Boolean.parseBoolean(config.get("recursive"));
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
                
                if (filePattern!=null) {
                	if (!filePattern.matcher(child.getFileName().toString()).matches()) {
                		continue;
                	}
                }
                
                // print out event
                System.out.format("%s: %s\n", kind, child);
                FolderEventBean bean = bean(kind, child);
  
                broadcaster.broadcast(bean);
                
                if (recursive && (kind == ENTRY_CREATE)) {
                	if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                		registerAll(child);
                	}
                }
                
			}
			
			boolean valid = key.reset();
		    if (!valid) throw new Exception("Cannot monitor '"+key+"'");
		}
		System.out.println("Finished folder monitor @ '"+dir+"'");
	}

	private FolderEventBean bean(Kind kind, Path child) {
		return bean(EventType.valueOf(kind.name()), child);
	}
	private FolderEventBean bean(EventType type, Path child) {
		
		FolderEventBean bean = new FolderEventBean(type, child.toAbsolutePath().toString());
		bean.setStatus(Status.NONE);
		try {
			bean.setHostName(InetAddress.getLocalHost().getHostName());
		} catch (UnknownHostException e) {
			// Not fatal but would be nice...
			e.printStackTrace();
		}
		bean.setUniqueId(System.currentTimeMillis()+"_"+UUID.randomUUID());
		bean.setUserName(System.getProperty("user.name"));
		bean.setRunDirectory(child.getParent().toAbsolutePath().toString());
		bean.setName(type.name());
		bean.setSubmissionTime(System.currentTimeMillis());
		
		return bean;
	}

	@Override
	public void stop() throws Exception {
		
		System.out.println("Stopping folder monitor @ '"+dir+"'");
		watcher.close();
		stopped = true;
		Thread.sleep(2000);
		connection.close();
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
		return "Folder monitoring '"+location+"'";
	}
}
