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

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;

/**
 * Represents a command to export data from a graph.
 *
 * @created Jun 23, 2008
 * @author Alex Hall
 * @copyright &copy; 2008 <a href="http://www.revelytix.com">Revelytix, Inc.</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Export extends DataOutputTx {

  /**
   * Creates a new Export command, exporting data from the graph URI to a file or output stream.
   * @param graphURI The graph to export.
   * @param destination The location to export the data. Only file URLs supported at the moment.
   *        May be null if an output stream will be provided.
   * @param local Set to <code>true</code> to indicate that the source is on the client system.
   */
  public Export(URI graphURI, URI destination, boolean local) {
    super(graphURI, destination, graphURI, local);
    if (graphURI == null) throw new IllegalArgumentException("Need a valid source graph URI");
  }
  
  /**
   * Alternate constructor for creating a command to export data from a graph to an output stream.
   * @param graphURI  The graph to export.
   * @param outputStream The stream that will receive the contents of the exported graph.
   */
  public Export(URI graphURI, OutputStream outputStream) {
    this(graphURI, null, true);
    setOverrideOutputStream(outputStream);
  }
  
  /**
   * Perform an export on a graph.
   * @param conn The connection to talk to the server on.
   * @return The text describing the graph that was exported.
   * @throws QueryException There was an error asking the server to perform the export.
   */
  public Object execute(Connection conn) throws QueryException {
    URI src = getSource();
    URI dest = getDestination();
    
    try {
      if (isLocal()) {
        getMarshalledData(conn);
      } else {
        conn.getSession().export(src, dest);
      } 
      
      if (logger.isDebugEnabled()) logger.debug("Completed backing up " + src + " to " + dest);
      
      return setResultMessage("Successfully exported " + src + " to " + 
          (dest != null ? dest : "output stream") + ".");
    }
    catch (IOException ioe) {
      logger.error("Error attempting to export: " + src, ioe);
      throw new QueryException("Error attempting to export: " + src, ioe);
    }
  }
  
  /**
   * Public interface to perform an export into an output stream.
   * This is callable directly, without an AST interface.
   * @param conn The connection to a server to perform the export.
   * @param graphURI The URI describing the graph on the server to export.
   * @param outputStream The output which will receive the data to be exported.
   * @throws QueryException There was an error asking the server to perform the export.
   */
  public static void export(Connection conn, URI graphURI, OutputStream outputStream) throws QueryException {
    Export export = new Export(graphURI, null, true);
    export.setOverrideOutputStream(outputStream);
    export.execute(conn);
  }

  /**
   * Perform the transfer with the configured data stream.
   */
  @Override
  protected void doTx(Connection conn, OutputStream outputStream) throws QueryException {
    conn.getSession().export(getSource(), outputStream);
  }

}