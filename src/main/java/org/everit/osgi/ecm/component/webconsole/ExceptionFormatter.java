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

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionFormatter {

    public String format(final Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
