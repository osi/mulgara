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

package org.mulgara.client.jena.kmodel;

//Java 2 standard packages
import java.net.*;
import java.util.*;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.util.iterator.*;

//Mulgara packages
import org.mulgara.itql.*;
import org.mulgara.itql.lexer.*;
import org.mulgara.itql.parser.*;
import org.mulgara.query.Answer;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.query.rdf.*;
import org.mulgara.server.*;
import org.mulgara.server.driver.*;


/**
 * A Jena Model backed by a Mulgara triplestore.
 *
 * <p>This class uses a Mulgara Session and ItqlInterpreter to emulate a Jena Model,
 * ultimately using RMI to communicate with the server.  It represents a single
 * thread's connection to a Mulgara model.</p>
 *
 * @created 2001-08-16
 *
 * @author Chris Wilper
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/02/02 21:12:06 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2003 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class KModel extends ModelCom implements Model {

  /**
   * If set, causes diagnostic messages to be sent to standard output, default
   * is false.
   */
  public static boolean DEBUG = false;

  /** Does all the work */
  private KGraph graph;

  /**
   * @see #getInstance(URI, URI)
   * @see #getInstance(URI, URI, URI)
   */
  private KModel(KGraph graph) {

    super(graph);
    this.graph = graph;
  }

  /**
   * Get a Jena Model instance given a Mulgara model's URI.
   *
   * <p>The model must already exist.</p>
   * <pre>
   *   URI serverURI = new URI("rmi://localhost/server1");
   *   URI modelURI = new URI("rmi://localhost/server1#model");
   *   Model jenaModel = KModel.getInstance(serverURI, modelURI);
   * </pre>
   *
   * @throws SessionFactoryFinderException if a connection to the server
   *         cannot be established.
   * @throws QueryException if a session couldn't be generated.
   */
  public static KModel getInstance(URI serverURI, URI modelURI)
      throws SessionFactoryFinderException, QueryException {

    return getInstance(serverURI, modelURI, null);
  }

  /**
   * Get a Jena Model instance given Mulgara model and fulltext model URIs.
   *
   * <p>All literals added or deleted in the model will correspondingly be
   * added or deleted in the text model.</p>
   * <p>The models must already exist.</p>
   * <p>Example:
   * <pre>
   *   URI serverURI = new URI("rmi://localhost/server1");
   *   URI modelURI = new URI("rmi://localhost/server1#model");
   *   URI textModelURI = new URI("rmi://localhost/server1#textmodel");
   *   Model jenaModel = KModel.getInstance(serverURI, modelURI, textModelURI);
   * </pre></p>
   *
   * @throws SessionFactoryFinderException if a connection to the server
   *         cannot be established.
   * @throws QueryException if either session couldn't be generated.
   */
  public static KModel getInstance(URI serverURI, URI modelURI, URI textModelURI)
      throws SessionFactoryFinderException, QueryException {

    //Find a Session for the Server
    Session modelSession = getSession(serverURI);
    ItqlInterpreter itql = new ItqlInterpreter(new HashMap());

    //has a text Model been specified
    Session textModelSession = null;

    if (textModelURI != null) {
      textModelSession = getSession(serverURI);
    }

    return new KModel(new KGraph(modelURI, modelSession, textModelURI,
        textModelSession, itql));
  }

  /**
   * Gets a new Session for the model's Server.
   *
   * @param serverURI the URI of the server to connect to.
   * @throws URISyntaxException
   * @throws SessionFactoryFinderException
   * @throws QueryException
   * @return Session
   */
  private static Session getSession(URI serverURI)
      throws SessionFactoryFinderException, QueryException {

    try {
      //get a factory
      SessionFactory factory = SessionFactoryFinder.newSessionFactory(serverURI);

      //create a new session
      return factory.newSession();
    }
    catch (NonRemoteSessionException nrse) {
      throw new QueryException("Did not recognise server as local", nrse);
    }
  }

  /**
   * Close underlying Mulgara session(s) and any unclosed iterators.
   */
  public void close() {

    // Graph takes care of closing the session(s)
    graph.close();
    super.close();
  }

  /**
   * Call close() at garbage collection time (in case it hasn't been closed).
   */
  protected void finalize() {

    close();
  }
}
