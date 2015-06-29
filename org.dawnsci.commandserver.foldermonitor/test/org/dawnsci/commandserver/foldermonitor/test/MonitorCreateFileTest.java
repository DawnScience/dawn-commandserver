package org.dawnsci.commandserver.foldermonitor.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawnsci.commandserver.core.beans.StatusBean;
import org.dawnsci.commandserver.foldermonitor.Monitor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MonitorCreateFileTest {
	
	private File    dir;
	private Monitor monitor;
		
	@Before
	public void monitor() throws Exception {
		
		// Clean directory for every test
		if (isWindowsOS()) {
			dir = new File("C:/tmp/monitorTest");
		} else {
			dir = new File("/scratch/monitorTest");
		}
		recursiveDelete(dir);
		dir.mkdirs();		

		
		monitor = new Monitor();
		final Map<String,String> conf = new HashMap<String,String>(2);
		conf.put("nio",       "true");
		conf.put("recursive", "true");
		conf.put("filePattern", ".*frames.mrc"); // Or whatever
		
		monitor.mock(conf, dir.toPath());
	}
	
	@After
	public void stop() throws Exception {
		if (monitor!=null) monitor.stop();
		monitor = null;
	}

	@Test
	public void testOneFile() throws Exception {
		testFiles(dir, "fredframes.mrc");
	}
	
	@Test
	public void testTwoFiles() throws Exception {
		testFiles(dir, "fred1frames.mrc", "fred2frames.mrc");
	}
	
	@Test
	public void testTwoFilesTwoIgnored() throws Exception {
		
		testFiles(dir, "fred1frames.mrc", "fred2frames.mrc", "ignore1.mrc", "ignore2.mrc");
	}

	/**
	 * 
	 * @param directory
	 * @param names, valid ones first, ones not matching the pattern last
	 * @throws Exception
	 */
	private void testFiles(File directory, String... names) throws Exception {
		
		final List<StatusBean> beans = new ArrayList<StatusBean>(1);
		mockBroadcasterNewFiles(beans);
		
		Thread.sleep(100);
		
		for (String name:names) {
			final File file = new File(directory, name);
			file.deleteOnExit();
			file.createNewFile();
		}
		
		Thread.sleep(100);
		
		for (String name : names) {
				
			if (beans.isEmpty()) break;
			
			final File file = new File(directory, name);
			try {
				String filePath = beans.remove(0).getProperties().getProperty("file_path");
				if (!filePath.equals(file.getAbsolutePath())) {
					throw new Exception(name+" not found!");
				}
				if (!name.matches(".*frames.mrc")) {
					throw new Exception("Unexpected invalid name "+name+"!");
				}
			} finally {
				file.delete();
			}
		}
	}

	@Test
	public void testSubDirsNoEvent() throws Exception {
		
		final List<StatusBean> beans = new ArrayList<StatusBean>(1);
		mockBroadcasterNewFiles(beans);
		
		Thread.sleep(100);
		
		final File subd1 = new File(dir, "subd1");
		subd1.mkdirs();
		
		final File subd2 = new File(dir, "subd2");
		subd2.mkdirs();

		try {
			Thread.sleep(100);
			
			if (!beans.isEmpty()) throw new Exception("Event for directory!");
			
		} finally {
			subd1.delete();
			subd2.delete();
		}
		if (!beans.isEmpty()) throw new Exception("Event for directory!");
			
	}
	
	@Test
	public void testSubDirsSubFiles() throws Exception {
		
		final List<StatusBean> beans = new ArrayList<StatusBean>(1);
		mockBroadcasterNewFiles(beans);
		
		Thread.sleep(100);
		
		final File subd1 = new File(dir, "subd1");
		subd1.mkdirs();
		
		final File subd2 = new File(dir, "subd2");
		subd2.mkdirs();

		try {
			Thread.sleep(100);
			
			if (!beans.isEmpty()) throw new Exception("Event for directory!");
			
			testFiles(subd1, "fred1frames.mrc", "fred2frames.mrc");
			testFiles(subd2, "fred1frames.mrc", "fred2frames.mrc", "ignore1.mrc", "ignore2.mrc");
		
		} finally {
			subd1.delete();
			subd2.delete();
		}
			
	}

	
	@Test
	public void testSubDirsNoEventMatchingName() throws Exception {
		
		final List<StatusBean> beans = new ArrayList<StatusBean>(1);
		mockBroadcasterNewFiles(beans);
		
		Thread.sleep(100);
		
		final File subd1 = new File(dir, "subd1frames.mrc");
		subd1.mkdirs();
		
		final File subd2 = new File(dir, "subd2frames.mrc");
		subd2.mkdirs();

		Thread.sleep(100);
		
		if (!beans.isEmpty()) throw new Exception("Event for directory!");

		subd1.delete();
		subd2.delete();
		
		if (!beans.isEmpty()) throw new Exception("Event for directory!");
	}

	/**
	 * Only interested in new files.
	 * @param beans
	 */
	private void mockBroadcasterNewFiles(final List<StatusBean> beans) {
		
		MockBroadcaster b = new MockBroadcaster(new StatusBroadcastListener() {
			@Override
			public void statusBroadcasted(StatusBean bean) {
				final String type = bean.getProperty("event_type");
				if (!type.equals("ENTRY_CREATE")) return;
				if (!"true".equals(bean.getProperty("is_file"))) return;
				beans.add(bean);
			}
		});
		monitor.setBroadcaster(b);
	}

	static private boolean isWindowsOS() {
		return (System.getProperty("os.name").indexOf("Windows") == 0);
	}
	
	static public final boolean recursiveDelete(File parent) {

		if (parent.exists()) {
			if (parent.isDirectory()) {

				File[] files = parent.listFiles();
				for (int ifile = 0; ifile < files.length; ++ifile) {
					if (files[ifile].isDirectory()) {
						recursiveDelete(files[ifile]);
					}
					if (files[ifile].exists()) {
						files[ifile].delete();
					}
				}
			}
			return parent.delete();
		}
		return false;
	}

}
