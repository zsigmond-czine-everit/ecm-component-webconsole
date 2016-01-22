/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.ecm.component.webconsole;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.expression.ExpressionCompiler;
import org.everit.expression.ParserConfiguration;
import org.everit.expression.jexl.JexlExpressionCompiler;
import org.everit.osgi.ecm.component.resource.ComponentContainer;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.everit.osgi.ecm.component.resource.ComponentState;
import org.everit.templating.CompiledTemplate;
import org.everit.templating.TemplateCompiler;
import org.everit.templating.html.HTMLTemplateCompiler;
import org.everit.templating.text.TextTemplateCompiler;
import org.everit.web.partialresponse.PartialResponseBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Webconsole plugin servlet that shows all ECM components and their states.
 */
public class ECMWebConsoleServlet implements Servlet {

  private static final ExceptionFormatter EXCEPTION_FORMATTER = new ExceptionFormatter();

  /**
   * In case there is a fragment suffix in the end of the URI, only the specified
   * {@link ComponentRevision} is rendered.
   */
  private static final String FRAGMENT_URI_SUFFIX = ".fragment";

  private static final int HTTP_NOT_FOUND = 404;

  private final BundleContext bundleContext;

  private final ClassLoader classLoader;

  private final CompiledTemplate componentsTemplate;

  private final ServiceTracker<ComponentContainer<?>, ComponentContainer<?>> containerTracker;

  private final URL partialResponseJSURL;

  private ServletConfig servletConfig;

  /**
   * Constructor.
   *
   * @param containerTracker
   *          The {@link ServiceTracker} that tracks all {@link ComponentContainer} services.
   * @param bundleContext
   *          The context of the bundle that contains this webconsole plugin.
   */
  public ECMWebConsoleServlet(
      final ServiceTracker<ComponentContainer<?>, ComponentContainer<?>> containerTracker,
      final BundleContext bundleContext) {
    this.containerTracker = containerTracker;
    this.bundleContext = bundleContext;
    classLoader = bundleContext.getBundle().adapt(BundleWiring.class).getClassLoader();
    partialResponseJSURL = resolvePartialResponseJSURL();

    ExpressionCompiler expressionCompiler = new JexlExpressionCompiler();

    TextTemplateCompiler textTemplateCompiler = new TextTemplateCompiler(expressionCompiler);

    Map<String, TemplateCompiler> inlineCompilers = new HashMap<String, TemplateCompiler>();
    inlineCompilers.put("text", textTemplateCompiler);

    HTMLTemplateCompiler htmlTemplateCompiler = new HTMLTemplateCompiler(expressionCompiler,
        inlineCompilers);

    ParserConfiguration parserConfiguration = new ParserConfiguration(classLoader);

    Map<String, Class<?>> variableTypes = new HashMap<String, Class<?>>();
    variableTypes.put("mp", MetaTypeProvider.class);
    parserConfiguration.setVariableTypes(variableTypes);

    componentsTemplate = htmlTemplateCompiler.compile(
        readResource("META-INF/webcontent/ecm_components.html"),
        parserConfiguration);

  }

  private ComponentStateSum addState(final ComponentStateSum result, final ComponentState state) {
    switch (state) {
      case ACTIVE:
        result.setActive(result.getActive() + 1);
        break;
      case UNSATISFIED:
        result.setUnsatisfied(result.getUnsatisfied() + 1);
        break;
      case FAILED:
        result.setFailed(result.getFailed() + 1);
        break;
      case STARTING:
        result.setStarting(result.getStarting() + 1);
        break;
      case STOPPING:
        result.setStopping(result.getStopping() + 1);
        break;
      case INACTIVE:
        result.setInactive(result.getInactive() + 1);
        break;
      case FAILED_PERMANENT:
        break;
      case UPDATING_CONFIGURATION:
        break;
      default:
        break;
    }
    return result;
  }

  private void addThreadViewerAvailablityToVars(final Map<String, Object> vars)
      throws ServletException {
    boolean threadViewerAvailable = false;
    try {
      threadViewerAvailable = bundleContext.getServiceReferences(Servlet.class.getName(),
          "(felix.webconsole.label=threads)") != null;
    } catch (InvalidSyntaxException e) {
      throw new ServletException(e);
    }

    vars.put("threadViewerAvailable", threadViewerAvailable);
  }

  private ComponentStateSum countStates(
      final SortedMap<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>> ccMap) {
    ComponentStateSum result = new ComponentStateSum();
    for (ComponentContainer<?> cc : ccMap.values()) {
      for (ComponentRevision<?> cr : cc.getResources()) {
        result = addState(result, cr.getState());
      }
    }

    return result;
  }

  @Override
  public void destroy() {
  }

  private void doAjax(final HttpServletRequest req, final HttpServletResponse resp
  // , final Writer writer, final Map<String, Object> vars
  ) {
    String action = req.getParameter("action");
    if (action == null) {
      return;
    }
    if ("suggest".equals(action)) {
      try (PartialResponseBuilder prb = new PartialResponseBuilder(resp)) {
        // TODO
        prb.append("#suggestion", "<p>Hello world!</p>");
      }
      // componentsTemplate.render(writer, vars, null);
    }
  }

  private ComponentContainer<?> findContainerByServiceId(
      final String serviceId) {

    Set<Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>>> entrySet;
    entrySet = containerTracker.getTracked().entrySet();

    Iterator<Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>>> iterator;
    iterator = entrySet.iterator();

    ComponentContainer<?> result = null;

    while ((result == null) && iterator.hasNext()) {
      Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>> entry = iterator.next();
      ServiceReference<ComponentContainer<?>> serviceReference = entry.getKey();
      if (serviceId.equals(String.valueOf(serviceReference.getProperty(Constants.SERVICE_ID)))) {
        result = entry.getValue();
      }
    }
    return result;
  }

  private BundleCapability findMatchingWiredCapability(final String filterString)
      throws InvalidSyntaxException {
    BundleWiring bundleWiring = bundleContext.getBundle().adapt(BundleWiring.class);
    List<BundleWire> requiredWires = bundleWiring.getRequiredWires("everit.webresource");
    Iterator<BundleWire> iterator = requiredWires.iterator();

    Filter filter = bundleContext.createFilter(filterString);
    BundleCapability result = null;
    while (iterator.hasNext() && (result == null)) {
      BundleWire wire = iterator.next();
      BundleCapability capability = wire.getCapability();
      if (filter.matches(capability.getAttributes())) {
        result = capability;
      }
    }
    return result;

  }

  private ComponentRevision<?> findRevision(final ComponentRevision<?>[] revisions,
      final String servicePid) {
    ComponentRevision<?> result = null;
    for (int i = 0; (i < revisions.length) && (result == null); i++) {
      ComponentRevision<?> componentRevision = revisions[i];
      if (servicePid.equals(componentRevision.getProperties().get(Constants.SERVICE_PID))) {
        result = componentRevision;
      }
    }
    return result;
  }

  private ClassLoader getClassLoaderByMatchingWire(final String filter)
      throws InvalidSyntaxException {
    BundleCapability capability = findMatchingWiredCapability(filter);

    BundleWiring bundleWiring = capability.getRevision().getBundle().adapt(BundleWiring.class);
    return bundleWiring.getClassLoader();
  }

  // TODO
  /**
   * Dummy.
   *
   * @param path
   *          dummy
   * @return dummy
   */
  public URL getResource(final String path) {
    if (path.endsWith("partialresponse.min.js")) {
      return partialResponseJSURL;
    }

    return null;
  }

  @Override
  public ServletConfig getServletConfig() {
    return servletConfig;
  }

  @Override
  public String getServletInfo() {
    return "ECM Component Webconsole Plugin";
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    servletConfig = config;

  }

  private boolean isAjaxRequest(final HttpServletRequest request) {
    String ajaxHeader = request.getHeader("x-partialresponse-ajax");
    return !((ajaxHeader == null) || Boolean.FALSE.toString().equalsIgnoreCase(ajaxHeader));
  }

  private String readResource(final String resourceName) {
    InputStream inputStream = classLoader.getResourceAsStream(resourceName);
    final int bufferSize = 1024;

    byte[] buf = new byte[bufferSize];
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

  private URL resolvePartialResponseJSURL() {
    ClassLoader cL;
    try {
      cL = getClassLoaderByMatchingWire("(name=org.everit.web.partialresponse)");
    } catch (InvalidSyntaxException e) {
      return null;
    }

    return cL.getResource(
        "META-INF/resources/org.everit.web.resources/partialresponse/1.0.0/partialresponse.min.js");
  }

  @Override
  public void service(final ServletRequest req, final ServletResponse resp)
      throws ServletException, IOException {

    // if (!((req instanceof HttpServletRequest) && (resp instanceof HttpServletResponse))) {
    // throw new ServletException("non-HTTP request or response");
    // }

    HttpServletRequest httpReq = (HttpServletRequest) req;
    HttpServletResponse httpResp = (HttpServletResponse) resp;

    httpResp.setContentType("text/html");

    PrintWriter writer = httpResp.getWriter();

    String appRoot = (String) httpReq.getAttribute("felix.webconsole.appRoot");
    String pluginRoot = (String) httpReq.getAttribute("felix.webconsole.pluginRoot");

    String requestURI = httpReq.getRequestURI();

    ComponentStateSum numberOfComponetntsByState = countStates(containerTracker.getTracked());

    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("ccMap", containerTracker.getTracked());
    vars.put("appRoot", appRoot);
    vars.put("pluginRoot", pluginRoot);
    vars.put("templateUtil", new TemplateUtil());
    vars.put("exceptionFormatter", EXCEPTION_FORMATTER);
    vars.put("numberOfComponetntsByState", numberOfComponetntsByState);
    vars.put("consoleUtil", new ECMWebConsoleUtil());

    // Iterator<Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>>> iterator =
    // containerTracker
    // .getTracked().entrySet().iterator();
    // iterator.next();
    // iterator.next();
    // Entry<ServiceReference<ComponentContainer<?>>, ComponentContainer<?>> entry =
    // iterator.next();
    // ComponentContainer<?> cc = entry.getValue();
    // ComponentRevision<?>[] cccr = cc.getResources();
    // cccr[0].getRequirements(null).get(0).getDirectives().get("objectClass");
    // ServiceReference<ComponentContainer<?>> sr = entry.getKey();
    // try {
    // ServiceReference<?>[] srarray = bundleContext
    // .getAllServiceReferences("org.osgi.service.cm.ConfigurationAdmin", null);
    // Object service = bundleContext.getService(srarray[0]);
    //
    // } catch (InvalidSyntaxException e) {
    // e.printStackTrace();
    // }

    if (!isAjaxRequest(httpReq) && requestURI.equals(pluginRoot)) {
      componentsTemplate.render(writer, vars, "content");
    } else if (isAjaxRequest(httpReq)) {
      doAjax(httpReq, httpResp/* , writer, vars */);
    } else if (requestURI.endsWith(FRAGMENT_URI_SUFFIX)) {
      addThreadViewerAvailablityToVars(vars);

      String serviceIdAndPid = requestURI.substring(pluginRoot.length() + 1,
          requestURI.length() - FRAGMENT_URI_SUFFIX.length());
      String[] split = serviceIdAndPid.split("\\/");

      ComponentContainer<?> container = findContainerByServiceId(split[0]);

      if (container == null) {
        httpResp.setStatus(HTTP_NOT_FOUND);
        return;
      }

      ComponentRevision<?>[] revisions = container.getResources();
      ComponentRevision<?> revision = null;
      if (split.length > 1) {
        revision = findRevision(revisions, split[1]);
      } else if (revisions.length == 1) {
        revision = revisions[0];
      }

      if (revision == null) {
        httpResp.setStatus(HTTP_NOT_FOUND);
        return;
      }

      vars.put("revision", revision);
      vars.put("container", container);

      componentsTemplate.render(writer, vars, "componentRevision");
    } else {
      httpResp.setStatus(HTTP_NOT_FOUND);
    }
  }
}
