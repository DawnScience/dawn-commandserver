package org.dawnsci.commandserver.ui.preference;

import org.dawnsci.commandserver.ui.Activator;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class CommandInitializer extends AbstractPreferenceInitializer {


	@Override
	public void initializeDefaultPreferences() {
		
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		// TODO This is not the final URI
		store.setDefault(CommandConstants.JMS_URI,          "tcp://sci-serv5.diamond.ac.uk:61616");

	}

}
