/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.biz)
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

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * Util class to show the template.
 */
public class TemplateUtil {

  private void appendStateToSBIfMatches(final StringBuilder sb, final int stateMask,
      final int expectedState,
      final String stateName) {
    if ((stateMask & expectedState) > 0) {
      if (sb.length() > 0) {
        sb.append("|");
      }
      sb.append(stateName);
    }
  }

  private String convertClauseToString(final String namespace,
      final Map<String, String> directives,
      final Map<String, Object> attributes) {
    StringBuilder sb = new StringBuilder(namespace);

    if (directives.size() > 0) {
      sb.append(";");
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Map<String, Object> castedDirectives = (Map) directives;
    sb.append(translateClauseMap(castedDirectives, ":="));

    if (attributes.size() > 0) {
      sb.append(";");
    }
    sb.append(translateClauseMap(attributes, "="));
    return sb.toString();
  }

  private String escapeClauseValue(final String text) {
    if (text == null) {
      return "";
    }
    return text.replace(";", "\\;").replace("\"", "\\\"").replace("\\", "\\\\");
  }

  /**
   * Formats and object to a String as it is shown in requirements and capabilities.
   */
  public String toString(final Object object) {
    if (object == null) {
      return "";
    }
    Class<? extends Object> objectType = object.getClass();
    if (objectType.isArray()) {
      StringBuilder sb = new StringBuilder("[");
      int length = Array.getLength(object);
      for (int i = 0; i < length; i++) {
        if (i > 0) {
          sb.append(", ");
        }
        Object entry = Array.get(object, i);
        if (entry != null) {
          sb.append(toString(entry));
        }
      }
      sb.append("]");
      return sb.toString();
    }
    if (object instanceof Capability) {
      Capability capability = (Capability) object;
      return convertClauseToString(capability.getNamespace(), capability.getDirectives(),
          capability.getAttributes());
    }
    if (object instanceof Requirement) {
      Requirement requirement = (Requirement) object;
      return convertClauseToString(requirement.getNamespace(), requirement.getDirectives(),
          requirement.getAttributes());
    }
    return String.valueOf(object);
  }

  /**
   * Formats a clause Map to a String.
   */
  public String translateClauseMap(final Map<String, Object> clauseMap, final String equalExpr) {
    StringBuilder sb = new StringBuilder();
    Set<Entry<String, Object>> clauseEntrySet = clauseMap.entrySet();
    boolean first = true;
    for (Entry<String, Object> clauseEntry : clauseEntrySet) {
      if (!first) {
        sb.append(";");
      }
      sb.append(clauseEntry.getKey()).append(equalExpr)
          .append(escapeClauseValue(toString(clauseEntry.getValue())));
      first = false;

    }
    return sb.toString();
  }

  /**
   * Translates a Bundle stateMask to a human readable format.
   */
  public String translateStateMask(final int stateMask) {
    StringBuilder sb = new StringBuilder();
    appendStateToSBIfMatches(sb, stateMask, Bundle.RESOLVED, "RESOLVED");
    appendStateToSBIfMatches(sb, stateMask, Bundle.STARTING, "STARTING");
    appendStateToSBIfMatches(sb, stateMask, Bundle.ACTIVE, "ACTIVE");
    appendStateToSBIfMatches(sb, stateMask, Bundle.STOPPING, "STOPPING");
    return sb.toString();
  }
}
