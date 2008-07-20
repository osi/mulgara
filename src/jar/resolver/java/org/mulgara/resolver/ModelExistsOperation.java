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
 * Contributor(s): This file, excluding the two lines of the execute method 
 *   is an original work developed by Netymon Pty Ltd.  Excluding these to
 *   lines this file is:
 *   Copyright (c) 2006 Netymon Pty Ltd.
 *   All Rights Reserved.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.resolver;

// Java 2 standard packages
import java.net.URI;

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.query.*;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.*;

class ModelExistsOperation implements Operation
{
  /** Logger.  */
  private static final Logger logger = Logger.getLogger(ModelExistsOperation.class.getName());

  private URI modelURI = null;

  private boolean result;


  ModelExistsOperation(URI modelURI) {
    this.modelURI = modelURI;
  }

  //
  // Methods implementing Operation
  //

  public void execute(OperationContext       operationContext,
                      SystemResolver         systemResolver,
                      DatabaseMetadata       metadata) throws Exception
  {
    try {
      long model = systemResolver.lookupPersistent(new URIReferenceImpl(modelURI));
      result = systemResolver.modelExists(model);
    } catch (LocalizeException le) {
      result = false;
    }
  }

  public boolean isWriteOperation() {
    return false;
  }

  
  boolean getResult() {
    return result;
  }
}
