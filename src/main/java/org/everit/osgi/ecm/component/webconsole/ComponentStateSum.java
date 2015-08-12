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

/**
 * Class for storing the sum of component states.
 *
 */
public class ComponentStateSum {

  private int active = 0;

  private int failed = 0;

  private int inactive = 0;

  private int starting = 0;

  private int stopping = 0;

  private int unsatisfied = 0;

  public int getActive() {
    return active;
  }

  public int getFailed() {
    return failed;
  }

  public int getInactive() {
    return inactive;
  }

  public int getStarting() {
    return starting;
  }

  public int getStopping() {
    return stopping;
  }

  public int getUnsatisfied() {
    return unsatisfied;
  }

  public void setActive(final int active) {
    this.active = active;
  }

  public void setFailed(final int failed) {
    this.failed = failed;
  }

  public void setInactive(final int inactive) {
    this.inactive = inactive;
  }

  public void setStarting(final int starting) {
    this.starting = starting;
  }

  public void setStopping(final int stopping) {
    this.stopping = stopping;
  }

  public void setUnsatisfied(final int unsatisfied) {
    this.unsatisfied = unsatisfied;
  }

}
