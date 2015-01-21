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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.expression.ExpressionCompiler;
import org.everit.expression.ParserConfiguration;
import org.everit.expression.mvel.MvelExpressionCompiler;
import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.templating.CompiledTemplate;
import org.everit.templating.html.HTMLTemplateCompiler;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

public class ECMWebConsoleServlet extends HttpServlet {

    /**
     * .
     */
    private static final long serialVersionUID = -187515087668802084L;

    private final ClassLoader classLoader;

    private final CompiledTemplate componentsTemplate;

    private final ServiceTracker<ComponentContainer<?>, ComponentContainer<?>> containerTracker;

    public ECMWebConsoleServlet(final ServiceTracker<ComponentContainer<?>, ComponentContainer<?>> containerTracker,
            final ClassLoader classLoader) {
        this.containerTracker = containerTracker;
        this.classLoader = classLoader;

        ExpressionCompiler expressionCompiler = new MvelExpressionCompiler();
        HTMLTemplateCompiler htmlTemplateCompiler = new HTMLTemplateCompiler(expressionCompiler);

        ParserConfiguration parserConfiguration = new ParserConfiguration(classLoader);
        componentsTemplate = htmlTemplateCompiler.compile(readResource("META-INF/webcontent/ecm_components.html"),
                parserConfiguration);

    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {

        Object appRoot = req.getAttribute("felix.webconsole.appRoot");

        PrintWriter writer = resp.getWriter();

        SortedMap<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>> tracked = containerTracker
                .getTracked();

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("ccMap", tracked);
        vars.put("appRoot", appRoot);

        componentsTemplate.render(writer, vars, "content");
        // writer.write("<table>");
        //
        // for (Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>> entry : trackedEntries) {
        // ServiceReference<ComponentContainer<?>> serviceReference = entry.getKey();
        // Object serviceId = serviceReference.getProperty("service.id");
        // ComponentContainer<?> componentContainer = entry.getValue();
        // ComponentMetadata componentMetadata = componentContainer.getComponentMetadata();
        //
        // writer.write("<tr>");
        // writer.write("<td><a href=\"" + appRoot + "/services/" + serviceId + "\">" + serviceId + "</a></td>");
        //
        // writer.write("<td>" + componentMetadata.getComponentId() + "</td>");
        // writer.write("</tr>");
        //
        // ComponentRevision[] componentRevisions = componentContainer.getComponentRevisions();
        // if (componentRevisions.length > 0) {
        // writer.write("<tr>");
        // for (ComponentRevision componentRevision : componentRevisions) {
        // writer.write("<td>" + componentRevision.getState() + "</td>");
        // if (componentRevision.getCause() != null) {
        // StringWriter sw = new StringWriter();
        // PrintWriter pw = new PrintWriter(sw);
        // componentRevision.getCause().printStackTrace(pw);
        // writer.write("<td><pre>" + sw.toString() + "</pre></td>");
        // }
        // }
        // writer.write("</tr>");
        // }
        // }
        // writer.write("</table>");
    }

    private String readResource(final String resourceName) {
        InputStream inputStream = classLoader.getResourceAsStream(resourceName);
        byte[] buf = new byte[1024];
        try {
            int r = inputStream.read(buf);
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            while (r > -1) {
                bout.write(buf, 0, r);
                r = inputStream.read(buf);
            }
            return new String(bout.toByteArray(), Charset.forName("UTF8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
