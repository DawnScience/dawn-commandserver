package org.dawnsci.commandserver.core.process;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public interface POSIX extends Library {

    POSIX INSTANCE = (POSIX) Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), POSIX.class);  
    
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

	/**
	 * 
	 * @param filename
	 * @param mode
	 * @return
	 */
	public int chmod(String filename, int mode);
	
	/**
	 * 
	 * @param filename
	 * @param user
	 * @param group
	 * @return
	 */
	public int chown(String filename, int user, int group);
	
	/**
	 * 
	 * @param oldpath
	 * @param newpath
	 * @return
	 */
	public int rename(String oldpath, String newpath);
	
	/**
	 * 
	 * @param oldpath
	 * @param newpath
	 * @return
	 */
	public int link(String oldpath, String newpath);
	
	/**
	 * 
	 * @param path
	 * @param mode
	 * @return
	 */
	public int mkdir(String path, int mode);
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	public int rmdir(String path);
}