import os
import sys

#print sys.version
#print sys.path

print "\n\n*************************************************************"

print 'Executable: '+str(sys.executable)

print sys.path

term = 'dawb'
print 'Searching for '+term+'...'
for p in sys.path:
	if term in p:
		print p
		
print "\n \n"

import java.lang as jl
print "Java classpath: \n"+jl.System.getProperty("java.class.path")
for p in jl.System.getProperty("java.class.path"):
	if "diffraction" in p:
		print p

print "cachedir: "+jl.System.getProperty("python.cachedir")

import scisoftpy as dnp
# #For powder calibration and integration
from uk.ac.diamond.scisoft.diffraction.powder import PowderCalibration as Cal
from uk.ac.diamond.scisoft.diffraction.powder import PowderCalibrationInfoImpl as PdrCalInf
from uk.ac.diamond.scisoft.diffraction.powder import SimpleCalibrationParameterModel# as SimpleCalPrmModel
from uk.ac.diamond.scisoft.analysis.crystallography import CalibrationFactory as Fac
from uk.ac.diamond.scisoft.analysis.diffraction.powder import PixelSplittingIntegration as PSI
#For outputting a NeXus file of the calibration result
from org.eclipse.dawnsci.analysis.api.persistence import IPersistenceService
from org.eclipse.dawnsci.analysis.api.persistence import IPersistentFile
from org.dawnsci.persistence import PersistenceServiceCreator
from com.fasterxml.jackson.core import JsonProcessingException


print "And now, Java classpath: \n"+jl.System.getProperty("java.class.path")

pdrCalInf_dir = str(dir(PdrCalInf))
print 'dir(PdrCalInf): '+pdrCalInf_dir
