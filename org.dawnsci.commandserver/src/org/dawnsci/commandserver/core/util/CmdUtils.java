/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.core.util;

import java.io.File;

public class CmdUtils {


	/**
	 * Attempts to get a uri to the data which works on linux and windows and MacOS if they have 
	 * mounted /dls/
	 * 
	 * @param runDirectory
	 * @return
	 */
	public static String getUri(String runDirectory) {
		String ret = getSanitizedPath(runDirectory);
		if (ret.contains(":")) return ret;
		return "file://"+runDirectory;
	}

	/**
	 * Tries to write the xinfo correctly even if the run is on windows.
	 * @param path
	 * @return
	 */
	public static String getSanitizedPath(String path) {
		if (isWindowsOS() && path.startsWith("/dls/")) {
			path = "\\\\Data.diamond.ac.uk\\"+path.substring(5);
		}
		return path;
	}
	
	public static boolean isWindowsOS() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("windows");
	}
	/**
	 * @return true if linux
	 */
	public static boolean isLinuxOS() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("linux");
	}
	
	public static boolean isMacOS() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().indexOf("mac") >= 0;
	}

	
	public static boolean browse(final String location) throws Exception {
		
		final String     dir  = CmdUtils.getSanitizedPath(location);
		
		final File resultsDir = new File(dir);
		if (resultsDir.exists()) return browse(resultsDir);
		
		return false;
	}

	public static boolean browse(File resultsDir) throws Exception {
		
		final ProcessBuilder pb = new ProcessBuilder();
		
		// Can adjust env if needed:
		// Map<String, String> env = pb.environment();
		pb.directory(resultsDir);
		
		if (isWindowsOS()) {
		    pb.command("cmd", "/C", "explorer \""+resultsDir.getAbsolutePath()+"\"");
		} else if (isLinuxOS()) {
		    pb.command("bash", "-c", "nautilus \""+resultsDir.getAbsolutePath()+"\"");
		} else if (isMacOS()) {
		    pb.command("/bin/sh", "-c", "open \""+resultsDir.getAbsolutePath()+"\"");
		}
		
		pb.start(); // We don't wait for it
 
		return true;
	}
}
