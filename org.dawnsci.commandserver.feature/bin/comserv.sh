#!/bin/sh
# Script to run a workflow in batch

# TODO MAKE GENERIC
./dawn -noSplash -application org.dawnsci.commandserver.consumer -data /scratch/CHANGEME -consolelog -os linux -ws gtk -arch $HOSTTYPE -consoleLog -uri tcp://ws097.diamond.ac.uk:61616 -submit scisoft.xia2.SUBMISSION_QUEUE -topic scisoft.xia2.STATUS_TOPIC -status scisoft.xia2.STATUS_QUEUE -bundle org.dawnsci.commandserver.mx -consumer org.dawnsci.commandserver.mx.consumer.MXSubmissionConsumer
