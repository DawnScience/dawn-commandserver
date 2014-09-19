/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
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
