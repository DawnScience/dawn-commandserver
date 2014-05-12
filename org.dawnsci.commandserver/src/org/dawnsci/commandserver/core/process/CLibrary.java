package org.dawnsci.commandserver.core.process;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public interface CLibrary extends Library {

    CLibrary INSTANCE = (CLibrary) Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), CLibrary.class);  
    
    /**
     * The process id of the current process
     * @return
     */
	int getpid();
	
	/**
	 * Kill a process by pid, signal can be 9.
	 * @param pid
	 * @param signal
	 */
	void kill(int pid, int signal);
	
	/**
	 * Kill a process by pid, signal can be 9.
	 * @param pid
	 * @param signal
	 */
	void killall(String name);

}