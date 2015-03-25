package org.dawnsci.commandserver.core.server;


import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

class PathValidationHandler extends AbstractHandler {

	@Override
	public void handle(String target, Request baseRequest,
			           HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		
		response.setContentType("text/plain;charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
		
		try {
			final String  path  = decode(request.getParameter("path"));
			final File    file  = new File(path);
			
			boolean ok = file.exists() && file.canRead() && file.canWrite();
			if (ok) {
				response.getWriter().println(String.valueOf(PathState.OK));
			} else if (!file.exists()){
				response.getWriter().println(String.valueOf(PathState.NON_EXISTING));
			} else if (!file.canRead()){
				response.getWriter().println(String.valueOf(PathState.NON_READABLE));
			} else if (!file.canRead()){
				response.getWriter().println(String.valueOf(PathState.NON_WRITABLE));
			}

		} catch (Exception ne) {
			response.getWriter().println(String.valueOf(PathState.INVALID));
		}
		
	}

	

	private String decode(String value) throws UnsupportedEncodingException {
		if (value==null) return null;
		return URLDecoder.decode(value, "UTF-8");
	}

}
