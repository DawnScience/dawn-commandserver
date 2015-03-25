package org.dawnsci.commandserver.mx.server;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathValidationServer {

	private static final Logger logger = LoggerFactory.getLogger(PathValidationServer.class);

	private int port = 8619;

	private Server server;

	public void start() throws Exception{
		
		this.server = new Server(port);		
        server.setHandler(new PathValidationHandler());
	    server.start();
	
		logger.warn("Started "+getClass().getSimpleName());
	}

	public void stop() throws Exception {
		server.stop();
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
