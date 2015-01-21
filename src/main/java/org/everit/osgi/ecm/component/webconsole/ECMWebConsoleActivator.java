/**
 * This file is part of Everit - ECM Component Webconsole.
 *
 * Everit - ECM Component Webconsole is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Everit - ECM Component Webconsole is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Everit - ECM Component Webconsole.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.everit.osgi.ecm.component.webconsole;

import java.util.Hashtable;

import javax.servlet.Servlet;

import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
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
        servletProps.put("felix.webconsole.css", "res/ui/config.css");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Class<ComponentContainer<?>> clazz = (Class) ComponentContainer.class;

        containerTracker = new ServiceTracker<ComponentContainer<?>, ComponentContainer<?>>(context, clazz, null);
        containerTracker.open();

        ClassLoader classLoader = context.getBundle().adapt(BundleWiring.class).getClassLoader();
        Servlet servlet = new ECMWebConsoleServlet(containerTracker, classLoader);
        servletSR = context.registerService(Servlet.class, servlet, servletProps);
    }

    @Override
    public void stop(final BundleContext context) {
        servletSR.unregister();
        containerTracker.close();
    }

}
