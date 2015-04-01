package org.dawnsci.commandserver.core.server;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

class FilePermissionHandler extends AbstractHandler {

	@Override
	public void handle(String target, Request baseRequest,
			           HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		response.setContentType("text/plain;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		
		try {
		    final String  spath  = decode(request.getParameter("path"));
			final Path     path  = Paths.get(spath);
			Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
							
			for (PosixFilePermission posixFilePermission : perms) {
				response.getWriter().println(String.valueOf(posixFilePermission));
			}

		} catch (Throwable ne) {
			response.getWriter().println("INALVID");
		}
	}

	

	private String decode(String value) throws UnsupportedEncodingException {
		if (value==null) return null;
		return URLDecoder.decode(value, "UTF-8");
	}

}
