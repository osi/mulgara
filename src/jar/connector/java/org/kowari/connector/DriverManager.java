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

package org.kowari.connector;


// Java 2 standard packages
import java.io.Serializable;

// Java 2 enterprise packages
import javax.resource.ResourceException;
import javax.resource.spi.*;

import org.kowari.query.QueryException;
import org.kowari.server.SessionFactory;

/**
 * CLASS TO DO
 */
public class DriverManager implements ConnectionManager, Serializable {

  /**
   * Description of the Field
   */
  private SessionFactory connectionFactory = null;

  //
  // Methods implementing ConnectionManager
  //

  /**
   * @param factory PARAMETER TO DO
   * @param info PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   * @throws ResourceException
   */
  public Object allocateConnection(ManagedConnectionFactory factory,
    ConnectionRequestInfo info) throws ResourceException {

    ManagedDriverFactory managedFactory = (ManagedDriverFactory) factory;

    if (connectionFactory == null) {

      connectionFactory =
        (SessionFactory) managedFactory.createConnectionFactory(this);
    }

    try {

      return connectionFactory.newSession();
    }
     catch (QueryException e) {

      ResourceException resourceException =
        new ResourceException("Couldn't create session");
      resourceException.initCause(e);
      throw resourceException;
    }
  }
}
