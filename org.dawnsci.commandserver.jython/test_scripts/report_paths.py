import os
import sys

#print sys.version
#print sys.path

print "\n\n*************************************************************"

print sys.executable
for p in sys.path:
	if "diffraction" in p:
		print p

import java.lang as jl
print "Java classpath: \n"+jl.System.getProperty("java.class.path")

import scisoftpy as dnp
# #For powder calibration and integration
from uk.ac.diamond.scisoft.diffraction.powder import PowderCalibration as Cal
# from uk.ac.diamond.scisoft.diffraction.powder import PowderCalibrationInfoImpl as PdrCalInf
# from uk.ac.diamond.scisoft.diffraction.powder import SimpleCalibrationParameterModel# as SimpleCalPrmModel
# from uk.ac.diamond.scisoft.analysis.crystallography import CalibrationFactory as Fac
# from uk.ac.diamond.scisoft.analysis.diffraction.powder import PixelSplittingIntegration as PSI
# #For outputting a NeXus file of the calibration result
# from org.dawb.common.services import IPersistenceService
# from org.dawb.common.services import IPersistentFile
# from org.dawnsci.persistence import PersistenceServiceCreator
# from com.fasterxml.jackson.core import JsonProcessingException


print "And now, Java classpath: \n"+jl.System.getProperty("java.class.path")
