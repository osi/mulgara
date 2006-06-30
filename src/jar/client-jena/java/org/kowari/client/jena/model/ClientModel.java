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
 * The Initial Developer of the Original Code is Andrew Newman (C) 2005
 * All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.kowari.client.jena.model;

//Java 2 standard packages
import java.net.*;
import java.util.*;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.util.iterator.*;

//Kowari packages
import org.kowari.itql.*;
import org.kowari.itql.lexer.*;
import org.kowari.itql.parser.*;
import org.kowari.query.Answer;
import org.kowari.query.Query;
import org.kowari.query.QueryException;
import org.kowari.query.rdf.*;
import org.kowari.server.*;
import org.kowari.server.driver.*;

/**
 * A Jena Model backed by a Kowari triplestore.
 *
 * <p>This class uses a Kowari Session and ItqlInterpreter to emulate a Jena Model,
 * ultimately using RMI to communicate with the server.  It represents a single
 * thread's connection to a Kowari model.</p>
 *
 * @created 2005-01-18
 *
 * @author Chris Wilper
 * @author Andrew Newman
 *
 * @version $Revision: 1.2 $
 *
 * @modified $Date: 2005/02/02 21:12:23 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2005 Andrew Newman
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ClientModel extends ModelCom implements Model {

  /** If set, causes diagnostic messages to be sent to standard output, default is false. */
  public static boolean DEBUG = false;

  /** Does all the work */
  private ClientGraph graph;

  public ClientModel(ClientGraph graph) {

    super(graph);
    this.graph = graph;
  }

  /**
   * Close underlying Kowari session(s) and any unclosed iterators.
   */
  public void close() {

    // Graph takes care of closing the session(s)
    graph.close();
  }

  /**
   * Call close() at garbage collection time (in case it hasn't been closed).
   */
  protected void finalize() {

    close();
  }
}
