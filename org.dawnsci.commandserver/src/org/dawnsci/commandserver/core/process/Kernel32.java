package org.dawnsci.commandserver.core.process;

import com.sun.jna.Library;
import com.sun.jna.Native;

interface Kernel32 extends Library {

    public static Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

    public int GetProcessId(Long hProcess);
}