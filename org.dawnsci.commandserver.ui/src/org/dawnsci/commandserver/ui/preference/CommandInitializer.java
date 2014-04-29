package org.dawnsci.commandserver.ui.preference;

import org.dawnsci.commandserver.ui.Activator;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class CommandInitializer extends AbstractPreferenceInitializer {


	@Override
	public void initializeDefaultPreferences() {
		
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		// TODO This is not the final URI
		store.setDefault(CommandConstants.JMS_URI,          "tcp://ws097.diamond.ac.uk:61616");
		store.setDefault(CommandConstants.SUBMISSION_QUEUE, "scisoft.xia2.SUBMISSION_QUEUE");
		store.setDefault(CommandConstants.STATUS_QUEUE,     "scisoft.xia2.STATUS_QUEUE");
		store.setDefault(CommandConstants.STATUS_TOPIC,     "scisoft.xia2.STATUS_TOPIC");

	}

}
