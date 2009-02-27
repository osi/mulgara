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

package org.mulgara.resolver.spi;

// Local packages
import org.mulgara.query.Cursor;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.store.nodepool.NodePool;

/**
 * A collection of localized RDF statements.
 *
 * @created 2004-04-21
 * @author <a href="http://staff.tucanatech.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:50 $ 
 * @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@tucanatech.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */

public interface Statements extends Cursor {

  /** A variable for the subject position in a triple */
  static final Variable SUBJECT = new Variable("subject");

  /** A variable for the predicate position in a triple */
  static final Variable PREDICATE = new Variable("predicate");

  /** A variable for the object position in a triple */
  static final Variable OBJECT = new Variable("object");


  /** Stopgap measure until the stringpool gets a <q>no match</q> value. */
  static final long NONE = NodePool.NONE;

  /** Return the subject node of the current triple. */
  public long getSubject() throws TuplesException;

  /** Return the predicate node of the current triple. */
  public long getPredicate() throws TuplesException;

  /** Return the object node of the current triple. */
  public long getObject() throws TuplesException;

  public Object clone();
}
