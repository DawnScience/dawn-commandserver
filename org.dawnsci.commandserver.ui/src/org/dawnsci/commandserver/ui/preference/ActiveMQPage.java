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
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Advanced configuration for connecting to ActiveMQ
 * @author fcp94556
 *
 */
public class ActiveMQPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage{
	
	// Do not change, referenced externally.
	public static final String ID = "org.dawnsci.commandserver.ui.activemqPage";

	public ActiveMQPage() {
		super();
		
		final IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		setPreferenceStore(store);
		setDescription("Preferences for connecting auto-processing reruns to the command server.");
	}

	@Override
	protected void createFieldEditors() {
		
	    final StringFieldEditor uri = new StringFieldEditor(CommandConstants.JMS_URI, "Comamnd Server", getFieldEditorParent());
	    addField(uri);

	}

	@Override
	public void init(IWorkbench workbench) {
		// TODO Auto-generated method stub
		
	}

}
