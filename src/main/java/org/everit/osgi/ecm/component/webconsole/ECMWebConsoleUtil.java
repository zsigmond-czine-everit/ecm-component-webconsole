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

import org.everit.osgi.ecm.component.ECMComponentConstants;
import org.everit.osgi.ecm.component.resource.ComponentRevision;
import org.osgi.resource.Wire;

/**
 * Util class for ECM web console.
 *
 */
public class ECMWebConsoleUtil {

  /**
   * Returns the ECM Id of a component, if it is an ECM component.
   *
   * @param revision
   *          target component
   * @return If the component is a factory created component revision, then the service PID is
   *         returned. If it is a simple component, it's component Id is returned. If the component
   *         is not an ECM component, null is returned.
   */
  public String getId(final ComponentRevision<?> revision) {
    String ecmId = (String) revision.getProperties()
        .get(ECMComponentConstants.SERVICE_PROP_COMPONENT_SERVICE_PID);
    if (ecmId == null) {
      ecmId = (String) revision.getProperties()
          .get(ECMComponentConstants.SERVICE_PROP_COMPONENT_ID);
    }
    return ecmId;
  }

  /**
   * Returns the ECM Id of a component, if it is an ECM component.
   *
   * @param wires
   *          an array of wires. We only use the first one.
   * @return If the component is a factory created component revision, then the service PID is
   *         returned. If it is a simple component, it's component Id is returned. If the component
   *         is not an ECM component, null is returned.
   */
  public String getId(final Wire[] wires) {
    String ecmId = (String) wires[0].getCapability().getAttributes()
        .get(ECMComponentConstants.SERVICE_PROP_COMPONENT_SERVICE_PID);
    if (ecmId == null) {
      ecmId = (String) wires[0].getCapability().getAttributes()
          .get(ECMComponentConstants.SERVICE_PROP_COMPONENT_ID);
    }
    return ecmId;
  }
}
