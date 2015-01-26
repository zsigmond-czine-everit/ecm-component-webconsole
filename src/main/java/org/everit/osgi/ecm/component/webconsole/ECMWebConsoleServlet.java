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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.expression.ExpressionCompiler;
import org.everit.expression.ParserConfiguration;
import org.everit.expression.mvel.MvelExpressionCompiler;
import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.templating.CompiledTemplate;
import org.everit.templating.TemplateCompiler;
import org.everit.templating.html.HTMLTemplateCompiler;
import org.everit.templating.text.TextTemplateCompiler;
import org.osgi.framework.Constants;
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

    private final ExceptionFormatter EXCEPTION_FORMATTER = new ExceptionFormatter();

    public ECMWebConsoleServlet(final ServiceTracker<ComponentContainer<?>, ComponentContainer<?>> containerTracker,
            final ClassLoader classLoader) {
        this.containerTracker = containerTracker;
        this.classLoader = classLoader;

        ExpressionCompiler expressionCompiler = new MvelExpressionCompiler();

        TextTemplateCompiler textTemplateCompiler = new TextTemplateCompiler(expressionCompiler);

        Map<String, TemplateCompiler> inlineCompilers = new HashMap<String, TemplateCompiler>();
        inlineCompilers.put("text", textTemplateCompiler);

        HTMLTemplateCompiler htmlTemplateCompiler = new HTMLTemplateCompiler(expressionCompiler, inlineCompilers);

        ParserConfiguration parserConfiguration = new ParserConfiguration(classLoader);
        componentsTemplate = htmlTemplateCompiler.compile(readResource("META-INF/webcontent/ecm_components.html"),
                parserConfiguration);

    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
            IOException {

        resp.setContentType("text/html");

        PrintWriter writer = resp.getWriter();

        String appRoot = (String) req.getAttribute("felix.webconsole.appRoot");
        String pluginRoot = (String) req.getAttribute("felix.webconsole.pluginRoot");

        String requestURI = req.getRequestURI();

        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("ccMap", containerTracker.getTracked());
        vars.put("appRoot", appRoot);
        vars.put("pluginRoot", pluginRoot);
        vars.put("exceptionFormatter", EXCEPTION_FORMATTER);

        if (requestURI.equals(pluginRoot)) {
            componentsTemplate.render(writer, vars, "content");
        } else if (requestURI.endsWith(".fragment")) {
            String serviceIdAndPid = requestURI.substring(pluginRoot.length() + 1,
                    requestURI.length() - ".fragment".length());
            String[] split = serviceIdAndPid.split("\\/");

            ComponentContainer<?> container = findContainerByServiceId(split[0]);

            if (container == null) {
                resp.setStatus(404);
                return;
            }

            ComponentRevision[] revisions = container.getComponentRevisions();
            ComponentRevision revision = null;
            if (split.length > 1) {
                revision = findRevision(revisions, split[1]);
            } else if (revisions.length == 1) {
                revision = revisions[0];
            }

            if (revision == null) {
                resp.setStatus(404);
                return;
            }

            vars.put("revision", revision);
            vars.put("container", container);

            componentsTemplate.render(writer, vars, "componentRevision");
        } else {
            resp.setStatus(404);
        }
    }

    private ComponentContainer<?> findContainerByServiceId(
            final String serviceId) {

        Set<Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>>> entrySet = containerTracker
                .getTracked().entrySet();

        Iterator<Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>>> iterator = entrySet.iterator();

        ComponentContainer<?> result = null;

        while (result == null && iterator.hasNext()) {
            Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>> entry = iterator.next();
            ServiceReference<ComponentContainer<?>> serviceReference = entry.getKey();
            if (serviceId.equals(String.valueOf(serviceReference.getProperty(Constants.SERVICE_ID)))) {
                result = entry.getValue();
            }
        }
        return result;
    }

    private ComponentRevision findRevision(final ComponentRevision[] revisions, final String servicePid) {
        ComponentRevision result = null;
        for (int i = 0; i < revisions.length && result == null; i++) {
            ComponentRevision componentRevision = revisions[i];
            if (servicePid.equals(componentRevision.getProperties().get(Constants.SERVICE_PID))) {
                result = componentRevision;
            }
        }
        return result;
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
