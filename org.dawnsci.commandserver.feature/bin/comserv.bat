REM Script to run a workflow in batch

REM # MAKE GENERIC
dawn -noSplash -application com.isencia.passerelle.workbench.model.launch -data C:/tmp/CHANGEME -consolelog -consoleLog -uri tcp://ws097.diamond.ac.uk:61616 -submit scisoft.xia2.SUBMISSION_QUEUE -topic scisoft.xia2.STATUS_TOPIC -status scisoft.xia2.STATUS_QUEUE -bundle org.dawnsci.commandserver.mx -consumer org.dawnsci.commandserver.mx.consumer.MXSubmissionConsumer
