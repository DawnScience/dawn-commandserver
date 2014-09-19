/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.mx.process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import org.dawnsci.commandserver.core.util.CmdUtils;
import org.dawnsci.commandserver.mx.beans.ProjectBean;
import org.dawnsci.commandserver.mx.beans.SweepBean;

/**
 * Class to write Xia2 file for MX rerun capability
 * 
 * 
 * 
 
Example:

BEGIN PROJECT cm4952v2
BEGIN CRYSTAL xThaumM1S21

BEGIN HA_INFO
ATOM s
!NUMBER_PER_MONOMER N
!NUMBER_TOTAL M
END HA_INFO

BEGIN WAVELENGTH SAD
WAVELENGTH 0.979490
END WAVELENGTH SAD

BEGIN SWEEP SWEEP1
WAVELENGTH SAD
DIRECTORY /dls/i04/data/2014/cm4952-2/20140423/thau/graeme
IMAGE Thaum_M1S2_1_0001.cbf
START_END 1 400
BEAM 208.80 213.22
END SWEEP SWEEP1

END CRYSTAL xThaumM1S21
END PROJECT cm4952v2


Example of data which could be processed for testing:

/dls/i03/data/2014/cm4950-2/processed/20140425/gw/thau1
 * 
 * 
 * @author fcp94556
 *
 */
public class Xia2Writer extends BufferedWriter {


	public static final String DEFAULT_FILENAME = "automatic.xinfo";

	/**
	 * 
	 * @param filePath
	 * @throws Exception
	 */
	public Xia2Writer(final String filePath) throws Exception {
		super(new FileWriter(getXia2File(filePath)));
	}
	
	/**
	 * 
	 * @param file
	 * @throws Exception
	 */
	public Xia2Writer(File file) throws Exception {
		super(new FileWriter(checkFile(file)));
	}

	private static File checkFile(File file) throws Exception{
		if (!file.getName().toLowerCase().endsWith(".xinfo")) throw new Exception("The Xia2 file must have the extension xinfo!");
		file.getParentFile().mkdirs();
		return file;
	}

	private static File getXia2File(String filePath) throws Exception {
		File file = new File(filePath);
		checkFile(file);
		return file;
	}

	/**
	 * Do not forget to close this buffered reader after calling the write
	 * method.
	 * 
	 * @param bean
	 * @throws Exception
	 */
	public void write(ProjectBean bean) throws Exception {
		
		if (bean.getSweeps()==null || bean.getSweeps().isEmpty()) {
			throw new Exception("Cannot write an xinfo file with no sweeps!");
		}
		
		write("BEGIN PROJECT "+bean.getProjectName());
		newLine();
		write("BEGIN CRYSTAL "+bean.getCystalName());
		newLine();
		newLine();
		
		write("BEGIN WAVELENGTH NATIVE");
		newLine();
		if (bean.isAnomalous()) {
			write("ATOM X");
			newLine();			
		}
		write("END WAVELENGTH NATIVE");
		newLine();

		int iSweep = 1;
		for (SweepBean sweep : bean.getSweeps()) {
			
			write("BEGIN SWEEP SWEEP"+iSweep);
			newLine();
			write("WAVELENGTH NATIVE");
			newLine();
			write("DIRECTORY "+CmdUtils.getSanitizedPath(sweep.getImageDirectory()));
			newLine();
			write("IMAGE "+sweep.getFirstImageName());
			newLine();
			write("START_END "+sweep.getStart()+" "+sweep.getEnd());
			newLine();
			write("END SWEEP SWEEP"+iSweep);
			newLine();
			newLine();

			++iSweep;
		}
		
		write("END CRYSTAL "+bean.getCystalName());
		newLine();
		write("END PROJECT "+bean.getProjectName());
		newLine();

	}


	/**
	 * 
	 * 
	 * Longer Example:
	  
BEGIN PROJECT AUTOMATIC
BEGIN CRYSTAL DEFAULT

BEGIN WAVELENGTH NATIVE
WAVELENGTH 0.979490
END WAVELENGTH NATIVE

BEGIN SWEEP SWEEP1
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_10_0001.cbf
START_END 1 400
BEAM 208.71 213.22
END SWEEP SWEEP1

BEGIN SWEEP SWEEP2
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_11_0001.cbf
START_END 1 400
BEAM 208.79 213.26
END SWEEP SWEEP2

BEGIN SWEEP SWEEP3
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_12_0001.cbf
START_END 1 400
BEAM 208.85 213.22
END SWEEP SWEEP3

BEGIN SWEEP SWEEP4
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_13_0001.cbf
START_END 1 400
BEAM 208.94 213.20
END SWEEP SWEEP4

BEGIN SWEEP SWEEP5
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_14_0001.cbf
START_END 1 400
BEAM 208.76 213.22
END SWEEP SWEEP5

BEGIN SWEEP SWEEP6
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_15_0001.cbf
START_END 1 400
BEAM 208.74 213.27
END SWEEP SWEEP6

BEGIN SWEEP SWEEP7
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_16_0001.cbf
START_END 1 400
BEAM 208.78 213.20
END SWEEP SWEEP7

BEGIN SWEEP SWEEP8
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_17_0001.cbf
START_END 1 400
BEAM 208.75 213.23
END SWEEP SWEEP8

BEGIN SWEEP SWEEP9
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_18_0001.cbf
START_END 1 400
BEAM 208.82 213.11
END SWEEP SWEEP9

BEGIN SWEEP SWEEP10
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_19_0001.cbf
START_END 1 400
BEAM 208.83 213.24
END SWEEP SWEEP10

BEGIN SWEEP SWEEP11
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_1_0001.cbf
START_END 1 400
BEAM 208.81 213.24
END SWEEP SWEEP11

BEGIN SWEEP SWEEP12
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_20_0001.cbf
START_END 1 400
BEAM 208.72 213.25
END SWEEP SWEEP12

BEGIN SWEEP SWEEP13
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_2_0001.cbf
START_END 1 400
BEAM 208.88 213.19
END SWEEP SWEEP13

BEGIN SWEEP SWEEP14
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_3_0001.cbf
START_END 1 400
BEAM 208.78 213.24
END SWEEP SWEEP14

BEGIN SWEEP SWEEP15
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_4_0001.cbf
START_END 1 400
BEAM 208.73 213.20
END SWEEP SWEEP15

BEGIN SWEEP SWEEP16
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_5_0001.cbf
START_END 1 400
BEAM 208.83 213.19
END SWEEP SWEEP16

BEGIN SWEEP SWEEP17
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_6_0001.cbf
START_END 1 400
BEAM 208.91 213.15
END SWEEP SWEEP17

BEGIN SWEEP SWEEP18
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_7_0001.cbf
START_END 1 400
BEAM 208.83 213.18
END SWEEP SWEEP18

BEGIN SWEEP SWEEP19
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_8_0001.cbf
START_END 1 400
BEAM 208.80 213.09
END SWEEP SWEEP19

BEGIN SWEEP SWEEP20
WAVELENGTH NATIVE
DIRECTORY /Volumes/GraemeData/data/i04-cm-20xthaum
IMAGE Thaum_M1S2_9_0001.cbf
START_END 1 400
BEAM 208.74 213.24
END SWEEP SWEEP20

END CRYSTAL DEFAULT
END PROJECT AUTOMATIC


	 */
}
