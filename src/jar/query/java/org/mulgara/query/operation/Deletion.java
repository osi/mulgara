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

import org.jrdf.graph.Triple;
import org.mulgara.connection.Connection;
import org.mulgara.query.Query;

/**
 * An AST element for deleting from a graph.
 * @created Aug 15, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Deletion extends Modification {
  
  /**
   * Create a deletion command for deleting a set of statements from a graph.
   * @param graph The graph to delete from.
   * @param statements The data to be deleted.
   */
  public Deletion(URI graph, Set<Triple> statements){
    super(graph, statements);
  }

  /**
   * Create an deletion command for deleting the results of a query from a graph.
   * @param graph The graph to delete from.
   * @param selectQuery The query to get data from for deletion.
   */
  public Deletion(URI graph, Query selectQuery){
    super(graph, selectQuery);
  }

  /**
   * Performs the deletion.
   * @param conn the session to delete the data from the graph in.
   * @return Text describing the action.
   */
  public Object execute(Connection conn) throws Exception {
    if (isSelectBased()) conn.getSession().delete(graph, getSelectQuery());
    else conn.getSession().delete(graph, getStatements());
    return setResultMessage("Successfully deleted statements from " + graph);
  }

}
