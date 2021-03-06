/*
 * Copyright 2009 DuraSpace.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mulgara.query.operation;

import java.net.URI;

import org.apache.log4j.Logger;
import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;


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

  /**
   * Create a new create graph command
   * @param graphUri The identifier for the graph
   * @param type The identifier for the graph type
   */
  public CreateGraph(URI graphUri, URI type) {
    super(graphUri);
    this.graphUri = graphUri;
    this.type = type;
  }
  
  /**
   * Create a new create graph command, using the default graph type.
   * @param graphUri The identifier for the graph
   */
  public CreateGraph(URI graphUri) {
    super(graphUri);
    this.graphUri = graphUri;
    this.type = null;
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
   * @param conn The connection to a session to create the graph in.
   * @return Text describing the outcome.
   */
  public Object execute(Connection conn) throws QueryException {
    if (logger.isDebugEnabled()) logger.debug("Creating new graph " + graphUri);
    conn.getSession().createModel(graphUri, type);
    return setResultMessage("Successfully created graph " + graphUri);
  }

}
