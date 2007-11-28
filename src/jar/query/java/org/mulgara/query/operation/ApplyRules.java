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
import java.rmi.RemoteException;

import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;
import org.mulgara.rules.InitializerException;
import org.mulgara.rules.RulesException;
import org.mulgara.rules.RulesRef;

/**
 * Represents a command to apply rules to a set of data.
 *
 * @created Aug 10, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ApplyRules extends ServerCommand {

  /** The graph containing the rules to be run. */
  private final URI ruleGraph;
  
  /** The graph containing the data to apply the rules to. */
  private final URI baseGraph;
  
  /** The graph to put the rule productions into. */
  private final URI destGraph;
  
  /**
   * Create a new rules command.
   * @param ruleGraph The graph containing the rules to be run.
   * @param baseGraph The graph containing the data to apply the rules to.
   * @param destGraph The graph to put the rule productions into.
   */
  public ApplyRules(URI ruleGraph, URI baseGraph, URI destGraph) {
    super(destGraph);
    this.ruleGraph = ruleGraph;
    this.baseGraph = baseGraph;
    this.destGraph = destGraph;
  }
  
  /**
   * @return the ruleGraph
   */
  public URI getRuleGraph() {
    return ruleGraph;
  }

  /**
   * @return the baseGraph
   */
  public URI getBaseGraph() {
    return baseGraph;
  }

  /**
   * @return the destGraph
   */
  public URI getDestGraph() {
    return destGraph;
  }

  /**
   * Apply rules using the given connection.
   * @param conn The connection to make the rule application on.
   * @return A string containing the result message.
   * @throws InitializerException The rules were not structured correctly.
   * @throws QueryException Unable to read the rules.
   * @throws RemoteException There was a connectivity problem with the server.
   * @throws RulesException There was an error with the application of the rules.
   */
  public Object execute(Connection conn) throws RemoteException, RulesException, QueryException, InitializerException {
    return execute(conn, conn);
  }

  /**
   * Apply rules using separate connections for getting rules and applying them.
   * @param conn The connection to apply the rules with.
   * @param ruleConn The connection to retrieve rules over.
   * @return A string containing the result message.
   * @throws InitializerException The rules were not structured correctly.
   * @throws QueryException Unable to read the rules.
   * @throws RemoteException There was a connectivity problem with the server.
   * @throws RulesException There was an error with the application of the rules.
   */
  public Object execute(Connection conn, Connection ruleConn) throws RemoteException, RulesException, QueryException, InitializerException {
    if (conn == null) throw new IllegalArgumentException("Connection may not be null");
    // get the structure from the rule model
    RulesRef rules = ruleConn.getSession().buildRules(ruleGraph, baseGraph, destGraph);
    // create apply the rules to the model
    conn.getSession().applyRules(rules);
    return setResultMessage("Successfully applied " + ruleGraph + " to " + baseGraph + (destGraph == baseGraph ? "" : " => " + destGraph));
  }

}
