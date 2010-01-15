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
