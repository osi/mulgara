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
 * Contributor(s): Andrew Newman.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.client.jena;

// Java 2 standard packages
import java.net.*;

// Log4J
import org.apache.log4j.Logger;

// Jena
import com.hp.hpl.jena.rdf.model.*;

//Local packages
import org.mulgara.server.*;
import org.mulgara.server.driver.*;
import org.mulgara.query.QueryException;
import org.mulgara.client.jena.exception.*;
import org.mulgara.client.jena.kmodel.*;
import org.mulgara.client.jena.model.*;


/**
 * Abstract factory that contains static methods for creating client-side
 * Jena Model's and Graphs.
 *
 * @created 2004-08-17
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 * @author Andrew Newman
 *
 * @version $Revision: 1.11 $
 *
 * @modified $Date: 2005/02/02 21:09:59 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class AbstractJenaFactory {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger = Logger.getLogger(AbstractJenaFactory.class.
      getName());

  /**
   * Get a Jena Model backed by a KModel implementation given the server and
   * model URI.
   *
   * <p>The model must already exist.</p>
   *
   * @param serverURI the server to connect to.
   * @param modelURI the URI of the model to use.
   * @throws JenaClientException if the model could not be created.
   * @return Model the KModel Jena representation of the model.
   */
  public static Model newKModel(URI serverURI, URI modelURI) throws JenaClientException {

    try {

      return KModel.getInstance(serverURI, modelURI);
    }
    catch (SessionFactoryFinderException sessionException) {

      throw new JenaClientException("Could not create model: " + modelURI,
          sessionException);
    }
    catch (QueryException queryException) {

      throw new JenaClientException("Could not create model: " + modelURI,
          queryException);
    }
  }

  /**
   * Get a Jena Model backed by a KModel implementation given server,
   * model and optional fulltext model (maybe null).
   *
   * <p>All literals added or deleted in the model will correspondingly be added
   * or deleted in the text model.</p>
   *
   * <p>The models must already exist.</p>
   *
   * @param serverURI the server to connect to.
   * @param modelURI the URI of the model to use.
   * @param textModelURI String Model for storing "Literal" triples
   * @throws JenaClientException if the model could not be created.
   * @return Model the KModel Jena representation of the model.
   */
  public static Model newKModel(URI serverURI, URI modelURI, URI textModelURI)
      throws JenaClientException {

    try {
      return KModel.getInstance(serverURI, modelURI, textModelURI);
    }
    catch (SessionFactoryFinderException sessionException) {

      throw new JenaClientException("Could not create model: " + modelURI,
          sessionException);
    }
    catch (QueryException queryException) {

      throw new JenaClientException("Could not create model: " + modelURI,
          queryException);
    }
  }

  /**
   * Get a default Jena Model backed by an RMI model given a server and model
   * URI.
   *
   * <p>The model must already exist.</p>
   *
   * @param serverURI the server to connect to.
   * @param modelURI the URI of the model to use.
   * @throws JenaClientException
   * @return Model Jena model
   */
  public static Model newModel(URI serverURI, URI modelURI) throws JenaClientException {
    JenaSession session = createServerSession(serverURI);
    ClientGraph graph = new ClientGraph(session, modelURI);
    return new ClientModel(graph);
  }

  /**
   * Creates a new JRDFSession.
   *
   * @param serverURI the server to connect to.
   * @throws JenaClientException any exceptions that occur when trying to make the
   *   connection.
   * @return the newly created session.
   */
  private static JenaSession createServerSession(URI serverURI) throws JenaClientException {
    try {
      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(
          serverURI, true);
      JenaSession session = (JenaSession) sessionFactory.newJenaSession();
      return session;
    }
    catch (SessionFactoryFinderException sffe) {
      throw new JenaClientException("Failed to connect to " + serverURI, sffe);
    }
    catch (NonRemoteSessionException nrse) {
      throw new JenaClientException("Failed to create a remote session", nrse);
    }
    catch (QueryException qe) {
      throw new JenaClientException("Failed to get new Jena session", qe);
    }
  }
}
