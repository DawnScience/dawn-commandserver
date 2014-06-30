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
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

	@Override
	public void init(Map<String, String> configuration) throws Exception {
		
		this.config = configuration;
		setUri(new URI(configuration.get("uri")));
		String queue = configuration.get("status");
		String topic = configuration.get("topic");
		
		this.broadcaster   = new Broadcaster(getUri(), queue, topic);

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
		

	}
	
	@Override
	public void start() throws Exception {
		
		startNotifications(); // Tell the GUI that we are alive
		
		boolean nio = Boolean.parseBoolean(config.get("nio"));
		if (nio) {
            startNio();
		} else {
			throw new Exception("Polling not implemented!");
		}
	}

	private void startNio() throws Exception {
		
		boolean recursive = Boolean.parseBoolean(config.get("recursive"));
		System.out.println("Starting nio folder monitor @ '"+dir+"'. Recursive is "+(recursive?"on":"off"));
		System.out.println("Folder monitor topic is '"+broadcaster.getTopicName()+"'");
		System.out.println("Folder monitor queue is '"+broadcaster.getQueueName()+"'");
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
		
		FolderEventBean bean = new FolderEventBean(EventType.valueOf(kind.name()), child.toAbsolutePath().toString());
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
		bean.setName(kind.name());
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
