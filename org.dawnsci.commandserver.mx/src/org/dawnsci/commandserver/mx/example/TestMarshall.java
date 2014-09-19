/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.commandserver.mx.example;

import org.dawnsci.commandserver.core.beans.Status;
import org.dawnsci.commandserver.mx.beans.ProjectBean;
import org.dawnsci.commandserver.mx.beans.SweepBean;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestMarshall {

	public static void main(String[] args) throws Exception {
		
		// We want to get the JSON string for this:
		SweepBean col = new SweepBean("fred", "d0000000001", 0, 100);
		ProjectBean bean = new ProjectBean();
		bean.addSweep(col);
		bean.setStatus(Status.SUBMITTED);
		bean.setPercentComplete(10);
		
		ObjectMapper mapper = new ObjectMapper();
		String jsonString = mapper.writeValueAsString(bean);
		
		System.out.println(jsonString);
		
		final ProjectBean beanBack = mapper.readValue(jsonString, ProjectBean.class);
		System.out.println("Read in equals written out = "+beanBack.equals(bean));
	}
}
