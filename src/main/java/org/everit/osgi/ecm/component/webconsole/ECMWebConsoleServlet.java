package org.everit.osgi.ecm.component.webconsole;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.metadata.ComponentMetadata;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class ECMWebConsoleServlet extends HttpServlet {

    /**
     * .
     */
    private static final long serialVersionUID = -187515087668802084L;

    private final ServiceTracker<ComponentContainer<?>, ComponentContainer<?>> containerTracker;

    public ECMWebConsoleServlet(final ServiceTracker<ComponentContainer<?>, ComponentContainer<?>> containerTracker) {
        this.containerTracker = containerTracker;
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {

        Object appRoot = req.getAttribute("felix.webconsole.appRoot");

        PrintWriter writer = resp.getWriter();

        SortedMap<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>> tracked = containerTracker
                .getTracked();

        Set<Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>>> trackedEntries = tracked.entrySet();

        writer.write("<table>");

        for (Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>> entry : trackedEntries) {
            ServiceReference<ComponentContainer<?>> serviceReference = entry.getKey();
            Object serviceId = serviceReference.getProperty("service.id");
            ComponentContainer<?> componentContainer = entry.getValue();
            ComponentMetadata componentMetadata = componentContainer.getComponentMetadata();

            writer.write("<tr>");
            writer.write("<td><a href=\"" + appRoot + "/services/" + serviceId + "\">" + serviceId + "</a></td>");

            writer.write("<td>" + componentMetadata.getComponentId() + "</td>");
            writer.write("</tr>");

            ComponentRevision[] componentRevisions = componentContainer.getComponentRevisions();
            if (componentRevisions.length > 0) {
                writer.write("<tr>");
                for (ComponentRevision componentRevision : componentRevisions) {
                    writer.write("<td>" + componentRevision.getState() + "</td>");
                    if (componentRevision.getCause() != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        componentRevision.getCause().printStackTrace(pw);
                        writer.write("<td><pre>" + sw.toString() + "</pre></td>");
                    }
                }
                writer.write("</tr>");
            }
        }
        writer.write("</table>");
    }
}
