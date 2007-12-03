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

import org.apache.log4j.Logger;
import org.mulgara.connection.Connection;

/**
 * Represents a command to create a new graph.
 * @created Aug 10, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class CreateGraph extends ServerCommand {

  /** The logger */
  private static final Logger logger = Logger.getLogger(CreateGraph.class.getName());

  /** The URI for the graph. */
  private final URI graphUri;
  
  /** The URI for the type of the graph. */
  private final URI type;
  
  public CreateGraph(URI graphUri, URI type) {
    super(graphUri);
    this.graphUri = graphUri;
    this.type = type;
  }
  
  /**
   * Get the URI of the graph to create.
   * @return the URI of the graph to create.
   */
  public URI getGraphUri() {
    return graphUri;
  }

  /**
   * Get the type of the graph to create.
   * @return the type of the graph.
   */
  public URI getType() {
    return type;
  }

  /**
   * Perform the action of creating the graph.
   * @param The connection to a session to create the graph in.
   * @return Text describing the outcome.
   */
  public Object execute(Connection conn) throws Exception {
    if (logger.isDebugEnabled()) logger.debug("Creating new model " + graphUri);
    conn.getSession().createModel(graphUri, type);
    return setResultMessage("Successfully created model " + graphUri);
  }

}