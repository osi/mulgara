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

import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;

/**
 * Represents a command to drop a graph.
 * @created Aug 10, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class DropGraph extends ServerCommand {

  /** The URI for the graph. */
  private final URI graphUri;
  
  public DropGraph(URI graphUri) {
    super(graphUri);
    this.graphUri = graphUri;
  }
  
  /**
   * Get the URI of the graph to drop.
   * @return the URI of the graph to drop.
   */
  public URI getGraphUri() {
    return graphUri;
  }

  /**
   * Performs the deletion.
   * @param conn the session to delete the graph in.
   * @return Text describing the action.
   */
  public Object execute(Connection conn) throws QueryException {
    conn.getSession().removeModel(graphUri);
    return setResultMessage("Successfully dropped graph " + graphUri);
  }

}
