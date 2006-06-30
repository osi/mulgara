/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is the Kowari Metadata Store.
 *
 * The Initial Developer of the Original Code is Plugged In Software Pty
 * Ltd (http://www.pisoftware.com, mailto:info@pisoftware.com). Portions
 * created by Plugged In Software Pty Ltd are Copyright (C) 2001,2002
 * Plugged In Software Pty Ltd. All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.connector;

// Java 2 enterprise packages
import javax.resource.spi.ManagedConnectionMetaData;

/**
 * CLASS TO DO
 */
public class ManagedDriverMetaData implements ManagedConnectionMetaData {

  /**
   * Gets the EISProductName attribute of the ManagedDriverMetaData object
   *
   * @return The EISProductName value
   */
  public String getEISProductName() {

    return "World Wide Web";
  }

  /**
   * Gets the EISProductVersion attribute of the ManagedDriverMetaData object
   *
   * @return The EISProductVersion value
   */
  public String getEISProductVersion() {

    return "2.0";
  }

  /**
   * Gets the MaxConnections attribute of the ManagedDriverMetaData object
   *
   * @return The MaxConnections value
   */
  public int getMaxConnections() {

    return 0;
  }

  /**
   * Gets the UserName attribute of the ManagedDriverMetaData object
   *
   * @return The UserName value
   */
  public String getUserName() {

    return "guest";
  }
}