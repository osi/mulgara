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

package org.mulgara.content.rio;

// Java 2 standard packages

// Third party packages
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.*;
import org.jrdf.parser.ParserBlankNodeFactory;

// Locally written packages

/**
 *
 * @created 2004-12-22
 *
 * @author David Makepeace
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/01/05 04:58:04 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ParserBlankNodeFactoryImpl implements ParserBlankNodeFactory {

  /** Logger. */
  private static final Logger logger =
      Logger.getLogger(ParserBlankNodeFactoryImpl.class.getName());

  /**
   * The sequence number of the next unlabeled blank node.
   */
  private long seqNo;


  ParserBlankNodeFactoryImpl() {
    clear();
  }

  /**
   * Create a blank node that is unique relative to this instance of the
   * ParserBlankNodeFactory.
   *
   * @return the newly created blank node value.
   */
  public BlankNode createBlankNode() throws GraphElementFactoryException {
    return new ParserBlankNodeImpl(seqNo++);
  }

  /**
   * Create a blank node that is only distinguished by the nodeID value.
   *
   * @return the newly created blank node value.
   */
  public BlankNode createBlankNode(String nodeID) throws GraphElementFactoryException {
    return new ParserBlankNodeImpl(nodeID);
  }

  public void clear() {
    seqNo = 1;
  }

}
