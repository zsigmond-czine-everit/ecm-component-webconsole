package org.everit.osgi.ecm.component.webconsole;

import java.util.Hashtable;

import javax.servlet.Servlet;

import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

public class ECMWebConsoleActivator implements BundleActivator {

    private ServiceTracker<ComponentContainer<?>, ComponentContainer<?>> containerTracker;
    private ServiceRegistration<Servlet> servletSR;

    @Override
    public void start(final BundleContext context) {
        Hashtable<String, String> servletProps = new Hashtable<String, String>();
        servletProps.put("felix.webconsole.label", "everit_ecm_component");
        servletProps.put("felix.webconsole.category", "Everit");
        servletProps.put("felix.webconsole.title", "ECM Components");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Class<ComponentContainer<?>> clazz = (Class) ComponentContainer.class;

        containerTracker = new ServiceTracker<ComponentContainer<?>, ComponentContainer<?>>(context, clazz, null);
        containerTracker.open();

        Servlet servlet = new ECMWebConsoleServlet(containerTracker);
        servletSR = context.registerService(Servlet.class, servlet, servletProps);
    }

    @Override
    public void stop(final BundleContext context) {
        servletSR.unregister();
        containerTracker.close();
    }

}
