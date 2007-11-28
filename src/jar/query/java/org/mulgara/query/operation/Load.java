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
import java.io.InputStream;
import java.net.URI;

import org.apache.log4j.Logger;
import org.mulgara.connection.Connection;
import org.mulgara.query.ModelResource;
import org.mulgara.query.QueryException;

/**
 * Represents a command to load data into a model.
 *
 * @created Aug 19, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Load extends DataTx {

  /** The logger */
  static final Logger logger = Logger.getLogger(Load.class.getName());
  
  /** Model resource form of the source URI */
  private final ModelResource srcRsc;

  /**
   * Build a load operation, loading data from one URI into a graph specified by another URI.
   * @param source The URI of the source of the RDF data.
   * @param destination The URI of the graph to receive the data.
   * @param local Set to <code>true</code> to indicate that the source is on the client system.
   */
  public Load(URI source, URI destination, boolean local) {
    super(source, destination, destination, local);
    srcRsc = new ModelResource(source);
  }


  /**
   * Load the data into the destination graph through the given connection.
   * @param conn The connection to load the data over.
   * @return The number of statements that were inserted.
   */
  public Object execute(Connection conn) throws Exception {
    URI src = getSource();
    URI dest = getDestination();
    try {
      long stmtCount = isLocal() ? sendMarshalledData(conn) : conn.getSession().setModel(dest, srcRsc);
      if (logger.isDebugEnabled()) logger.debug("Loaded " + stmtCount + " statements from " + src + " into " + dest);
  
      if (stmtCount > 0L) setResultMessage("Successfully loaded " + stmtCount + " statements from " + src + " into " + dest);
      else setResultMessage("WARNING: No valid RDF statements found in " + src);
      
      return stmtCount;
      
    } catch (IOException ex) {
      logger.error("Error attempting to load : " + src, ex);
      throw new QueryException("Error attempting to load : " + src, ex);
    }
  }


  /**
   * Perform the transfer with the configured datastream.
   * @return The number of statements affected, or <code>null</code> if this is not relevant.
   */
  protected Long doTx(Connection conn, InputStream inputStream) throws QueryException {
    return conn.getSession().setModel(inputStream, getDestination(), srcRsc);
  }

}
