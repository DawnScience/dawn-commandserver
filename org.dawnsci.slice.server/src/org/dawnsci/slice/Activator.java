package org.dawnsci.slice;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static BundleContext context;
	@Override
	public void start(BundleContext c) throws Exception {
        context = c;
 	}

	@Override
	public void stop(BundleContext context) throws Exception {
        context = null;
	}

	public static Bundle getBundle(String location) {
		return context.getBundle(location);
	}
}
