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
 * Contributor(s):
 *    XAResource addition copyright 2008 The Topaz Foundation
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.server.beep;

// Java 2 Standard Packages
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.transaction.xa.XAResource;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.ByteOutputDataStream;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.Message;
import org.beepcore.beep.lib.Reply;
import org.jrdf.graph.Triple;
import org.mulgara.query.Answer;
import org.mulgara.query.ModelExpression;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.rules.RulesRef;
import org.mulgara.server.Session;
import org.mulgara.sparql.protocol.StreamFormatException;

/**
 * Network transport of a Mulgara {@link Session} via a BEEP-based application
 * protocol.
 *
 * In addition to BEEP's own reserved session management channel (Channel 0),
 * another channel (Channel 1) is maintained for the life of the session
 * using the {@link CommandProfile}.  This command channel is used for methods
 * without large parameters or return values.  For methods that may require
 * large amounts of traffic, per-method channels are opened using
 * special-purpose profiles:
 * <ul><li>{@link QueryProfile} for the {@link #query} method</li></ul>
 *
 * @created 2004-03-21
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/06/26 12:48:13 $
 *
 * @maintenanceAuthor $Author: pgearon $
 * @maintenanceAuthor $Author: pgearon $
 * @maintenanceAuthor $Author: pgearon $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class BEEPSession implements Session {
  /**
   * Logger.
   *
   * This is named after the class.
   */
  private static final Logger logger = Logger.getLogger(BEEPSession.class);

  /**
   * The wrapped BEEP session.
   */
  private org.beepcore.beep.core.Session beepSession;

  /**
   * The BEEP channel maintained to the server using the command profile.
   *
   * This is used for all small, non-streamed commands.
   */
  private Channel commandChannel;

  //
  // Constructor
  //

  /**
   * Create a Mulgara session by wrapping a BEEP session.
   *
   * @param beepSession  the BEEP session to wrap
   * @throws BEEPException if the initial command channel can't be opened to the
   *   <var>beepSession</var>
   * @throws IllegalArgumentException if <var>beepSession</var> is
   *   <code>null</code>
   */
  BEEPSession(org.beepcore.beep.core.Session beepSession) throws BEEPException {
    // Validate the "beepSession" parameter
    if (beepSession == null) {
      throw new IllegalArgumentException("Null \"beepSession\" parameter");
    }

    // Initialize the wrapped field
    this.beepSession = beepSession;

    // Open the command channel
    commandChannel = beepSession.startChannel(CommandProfile.URI);
    assert commandChannel != null;
  }

  //
  // Methods implementing Session
  //

  /**
   * Insert RDFStatements into a model.
   *
   * @param modelURI The URI of the model to insert into.
   * @param statements The Set of RDFStatements to insert into the model.
   * @throws QueryException if the insert cannot be completed.
   */
  public void insert(URI modelURI, Set statements) throws QueryException {
    throw new QueryException("Insert not implemented");
  }

  /**
   * Insert statements from one model into another model.
   *
   * @param modelURI URI The URI of the model to insert into.
   * @param query The query to perform on the server.
   * @throws QueryException if the insert cannot be completed.
   */
  public void insert(URI modelURI, Query query) throws QueryException {
    throw new QueryException("Insert not implemented");
  }

  /**
   * Delete RDFStatements from a model.
   *
   * @param modelURI The URI of the model to delete from.
   * @param statements The Set of RDFStatements to delete from the model.
   * @throws QueryException if the deletion cannot be completed.
   */
  public void delete(URI modelURI, Set statements) throws QueryException {
    throw new QueryException("Delete not implemented");
  }

  /**
   * Delete statements from a model.
   *
   * @param modelURI The URI of the model to delete from.
   * @param query The query to perform on the server.
   * @throws QueryException if the deletion cannot be completed.
   */
  public void delete(URI modelURI, Query query) throws QueryException {
    throw new QueryException("Delete not implemented");
  }

  /**
   * Backup all the data on the specified server. The database is not changed by
   * this method.
   *
   * @param serverURI The URI of the server to backup.
   * @param destinationURI The URI of the file to backup into.
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI serverURI, URI destinationURI) throws QueryException {
    throw new QueryException("Backup not implemented");
  }


  /**
   * Backup all the data on the specified server to an output stream.
   *  The database is not changed by this method.
   *
   * @param serverURI The URI of the server to backup.
   * @param outputStream The stream to receive the contents
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI serverURI, OutputStream outputStream) throws QueryException {
    throw new QueryException("Backup not implemented");
  }
  
  
  /**
   * Export the data in the specified graph. The database is not changed by this method.
   * 
   * @param graphURI The URI of the graph to export.
   * @param destinationURI The URI of the file to export into.
   * @throws QueryException if the export cannot be completed.
   */
  public void export(URI graphURI, URI destinationURI) throws QueryException {
    throw new QueryException("Export not implemented");
  }
  
  
  /**
   * Export the data in the specified graph to an output stream.
   * The database is not changed by this method.
   * 
   * @param graphURI The URI of the server or model to export.
   * @param outputStream The stream to receive the contents
   * @throws QueryException if the export cannot be completed.
   */
  public void export(URI graphURI, OutputStream outputStream) throws QueryException {
    throw new QueryException("Export not implemented");
  }


  /**
   * Restore all the data on the specified server. If the database is not
   * currently empty then the database will contain the union of its current
   * content and the content of the backup file when this method returns.
   *
   * @param serverURI The URI of the server to restore.
   * @param sourceURI The URI of the backup file to restore from.
   * @throws QueryException if the restore cannot be completed.
   */
  public void restore(URI serverURI, URI sourceURI) throws QueryException {
    throw new QueryException("Restore not implemented");
  }

  /**
   * Restore all the data on the specified server. If the database is not
   * currently empty then the database will contain the union of its current
   * content and the content of the backup file when this method returns.
   *
   * @param inputStream a client supplied inputStream to obtain the restore
   *        content from. If null assume the sourceURI has been supplied.
   * @param serverURI The URI of the server to restore.
   * @param sourceURI The URI of the backup file to restore from.
   * @throws QueryException if the restore cannot be completed.
   */
  public void restore(InputStream inputStream, URI serverURI, URI sourceURI) throws QueryException {
    throw new QueryException("Restore not implemented");
  }

  /**
   * Make a TQL query.
   *
   * @param query the query
   * @return a non-<code>null</code> answer to the <var>query</var>
   * @throws QueryException if <var>query</var> can't be answered
   */
  public Answer query(Query query) throws QueryException {
    // Open a channel using the Query profile
    Channel queryChannel;
    try {
      queryChannel = beepSession.startChannel(QueryProfile.URI);
    } catch (BEEPException e) {
      throw new QueryException("Couldn't open BEEP channel using Query profile", e);
    }
    assert queryChannel != null;

    // Beyond this point, we have to ensure that the channel gets closed.

    try {
      // Generate the query message
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(query);
      oos.close();

      // Send the query message
      Reply reply = new Reply();
      queryChannel.sendMSG(new ByteOutputDataStream(baos.toByteArray()), reply);

      // Process the reply to the query message
      assert reply.hasNext();
      Message message = reply.getNextReply();
      switch (message.getMessageType()) {
        case Message.MESSAGE_TYPE_RPY:

          // The BEEPAnswer wrapper will close the channel once
          // BEEPAnswer.close() is called.
          return new BEEPAnswer(message);

        case Message.MESSAGE_TYPE_ERR:
          try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                message.getDataStream().getInputStream()
                ));
            throw new QueryException(reader.readLine());
          } catch (IOException e) {
            throw new QueryException("Unidentified error on BEEP channel");
          }

          default:
            throw new QueryException(
                "Unexpected BEEP message type " + message.getMessageType()
                );
      }
    // Only leave the BEEP channel unclosed if a BEEPAnswer capable of taking
    // responsibility for doing this was returned.  If exiting exceptionally,
    // the channel must be explicitly closed.
    } catch (BEEPException e) {
      close(queryChannel);
      throw new QueryException("Couldn't communicate with BEEP server", e);
    } catch (IOException e) {
      close(queryChannel);
      throw new QueryException("Couldn't generate BEEP query", e);
    } catch (QueryException e) {
      close(queryChannel);
      throw e;
    } catch (StreamFormatException e) {
      close(queryChannel);
      throw new QueryException("Couldn't interpret BEEP reply", e);
    }
  }

  /**
   * Make a list of TQL queries.
   *
   * @param queries the list of queries
   * @return a list of non-<code>null</code> answers to the <var>queries</var>
   * @throws QueryException if <var>query</var> can't be answered
   */
  @SuppressWarnings("unchecked")
  public List query(List queries) throws QueryException {
    throw new QueryException("Multiple queries not implemented");
  }

  /**
   * Test the model for the occurrence of the triple.  A null value for any
   * of the parts of a triple are treated as unconstrained, any values will be
   * returned.
   *
   * @param modelURI URI of the model to be checked
   * @param triple Triple to be found
   * @throws QueryException if the model cannot be checked
   * @return boolean true if the model contains the triple.
   */
  public boolean contains(URI modelURI, Triple triple) throws QueryException {
    throw new QueryException("Contains not implemented.");
  }

  /**
   * Returns an answer containing a set of statements in the model that match a
   * given triple. A null value for any of the parts of a triple are treated as
   * unconstrained, any values will be returned.
   *
   * @param modelURI URI of the model to be searched
   * @param triple Triple constraint used to match the triples
   * @throws QueryException if the model cannot be searched
   * @return Answer containing the triples that matcth the constraint
   */
  public Answer find(URI modelURI, Triple triple) throws QueryException {
    throw new QueryException("Find not implemented.");
  }


  /**
   * Create a new model.
   *
   * @param modelURI the {@link URI} of the new model
   * @param modelTypeURI the {@link URI} identifying whether the new model is
   *   backed by a triple store (<code>mulgara:Model</code>) or by a Lucene full
   *   text index (<code>mulgara:LuceneModel</code>)
   * @throws QueryException if the model can't be created
   */
  public void createModel(URI modelURI, URI modelTypeURI) throws QueryException {
    throw new QueryException("Create Model not implemented");
  }

  /**
   * Remove an existing model.
   *
   * @param uri the {@link URI} of the doomed model
   * @throws QueryException if the model can't be removed
   */
  public void removeModel(URI uri) throws QueryException {
    throw new QueryException("Remove model not implemented");
  }

  public boolean modelExists(URI uri) throws QueryException {
    throw new QueryException("Remove model not implemented");
  }

  /**
   * Define the contents of a model.
   *
   * @param uri the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return The number of statements inserted into the model
   * @throws QueryException if the model can't be modified
   */
  public long setModel(URI uri, ModelExpression modelExpression) throws QueryException {
    throw new QueryException("Set Model not implemented");
  }

  /**
   * Define the contents of a model.
   *
   * @param inputstream a remote inputstream
   * @param uri the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return The number of statements inserted into the model
   * @throws QueryException if the model can't be modified
   */
  public long setModel(InputStream inputstream, URI uri, ModelExpression modelExpression) throws QueryException {
    throw new QueryException("Set Model not implemented");
  }


  /**
   * Sets the AutoCommit attribute of the Session object
   *
   * @param autoCommit The new AutoCommit value
   * @throws QueryException EXCEPTION TO DO
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException {
    /*
    try {
      // Send the command message
      Reply reply = new Reply();
      commandChannel.sendMSG(new StringOutputDataStream("autocommit "+autoCommit), reply);

      // Process the reply to the query message
      assert reply.hasNext();
      Message message = reply.getNextReply();
      switch (message.getMessageType()) {
        case Message.MESSAGE_TYPE_NUL:
          return;

        case Message.MESSAGE_TYPE_ERR:
          try {
            BufferedReader reader =
                new BufferedReader(new InputStreamReader(message.getDataStream().getInputStream()));
            throw new QueryException(reader.readLine());
          } catch (IOException e) {
            throw new QueryException("Unidentified error on BEEP channel");
          }

        default:
          throw new QueryException("Unexpected BEEP message type "+message.getMessageType());
      }
    }
    catch (BEEPException e) {
      throw new QueryException("Couldn't communicate with BEEP server", e);
    }
     */
  }

  /**
   * METHOD TO DO
   *
   * @throws QueryException EXCEPTION TO DO
   */
  public void commit() throws QueryException {
    throw new QueryException("Commit not implemented");
  }

  /**
   * METHOD TO DO
   *
   * @throws QueryException EXCEPTION TO DO
   */
  public void rollback() throws QueryException {
    throw new QueryException("Rollback not implemented");
  }

  /**
   * Release resources associated with this session.
   *
   * The session won't be usable after this method is invoked.
   */
  public void close() {
    try {
      beepSession.close();
    } catch (BEEPException e) {
      logger.warn("Unable to close BEEP session", e);
    }

    beepSession = null;
  }

  public void login(URI securityDomain, String username, char[] password) {
    logger.error("Login not implemented");
  }

  //
  // Internal methods
  //

  /**
   * This method attempts to close a BEEP channel.
   *
   * If it fails, it only logs the fact rather than throwing an exception.
   *
   * @param channel  a BEEP channel to close
   */
  private void close(Channel channel) {
    try {
      channel.close();
    } catch (BEEPException e) {
      logger.warn("Unable to close BEEP channel (ignoring)", e);
    }
  }

  public boolean isLocal() {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public RulesRef buildRules(URI ruleModel, URI baseModel, URI destModel) throws QueryException, org.mulgara.rules.InitializerException {
    throw new UnsupportedOperationException("This operation is only supported on local sessions.");
  }

  /**
   * {@inheritDoc}
   */
  public void applyRules(RulesRef rules) {
    throw new UnsupportedOperationException("This operation is only supported on local sessions.");
  }

  public XAResource getXAResource() throws QueryException {
    throw new QueryException("External transactions not implemented under Beep");
  }

  public XAResource getReadOnlyXAResource() throws QueryException {
    throw new QueryException("External transactions not implemented under Beep");
  }

  public boolean ping() {
    return true;
  }
}
