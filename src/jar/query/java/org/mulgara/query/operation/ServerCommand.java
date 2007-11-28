/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/osl-3.0.txt
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 */
package org.mulgara.query.operation;

import java.net.URI;
import java.util.Set;

import org.mulgara.query.ModelResource;

/**
 * An AST element for server-based commands.
 *
 * @created 2007-08-22
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class ServerCommand implements Command {

  /** The graph being referred to on the server. */
  private final ModelResource serverGraph;
  
  /** The message set by the result of this command. */
  private String resultMessage;
  
  /**
   * Creates a new command, with a principle graph URI.
   * @param serverGraphUri The URI of the graph.
   */
  public ServerCommand(URI serverGraphUri) {
    serverGraph = new ModelResource(serverGraphUri);
    resultMessage = "";
  }
  
  
  /**
   * Finds the server URI for the graph.
   * @return The URI used to find the server.
   */
  public URI getServerURI() {
    Set<URI> gs = serverGraph.getDatabaseURIs();
    assert gs.size() == 1;
    return gs.iterator().next();
  }


  /**
   * Indicates that the command modifies the state in a transaction.
   * @return <code>true</code> If the transaction state is to be modified.
   */
  public boolean isTxCommitRollback() {
    return false;
  }
  

  /**
   * Indicates that this operation is not local.
   * @return Always <code>false</code> to indicate this command is not local.
   */
  public final boolean isLocalOperation() {
    return false;
  }

  
  /**
   * Indicates that this operation is not a UI command by default.
   * @return By default this will be <code>false</code> as a server side operation should not
   *         affect the local UI.
   */
  public boolean isUICommand() {
    return false;
  }


  /**
   * Gets a message text relevant to the operation.  Useful for the UI.
   * @return A text message associated with the result of this
   * operation.
   */
  public String getResultMessage() {
    return resultMessage;
  }


  /**
   * Indicates that this command returns an Answer. Saves the overhead of checking
   * the return type of execute.
   * @return <code>false</code> by default.
   */
  public boolean isAnswerable() {
    return false;
  }


  /**
   * Sets message text relevant to the operation.  Useful for the UI.
   * @return The set text.
   */
  String setResultMessage(String resultMessage) {
    return this.resultMessage = resultMessage;
  }
}
