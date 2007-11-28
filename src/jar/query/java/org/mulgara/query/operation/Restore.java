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

import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;

/**
 * Represents a command to reload backup data.
 *
 * @created Aug 19, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Restore extends DataTx {

  public Restore(URI source, URI destination, boolean local) {
    super(source, destination, destination, local);
  }

  /**
   * The destination of a restore command is a database, not a graph.
   * @return The URI of the destination server.
   */
  public URI getServerURI() {
    return getDestination();
  }

  /**
   * Restore the data into the destination graph through the given connection.
   * @param conn The connection to restore the data over.
   * @return A text string describing the operation.
   */
  public Object execute(Connection conn) throws Exception {
    URI src = getSource();
    URI dest = getDestination();
    try {
      if (isLocal()) sendMarshalledData(conn);
      else conn.getSession().restore(dest, src);

      if (logger.isDebugEnabled()) logger.debug("Completed restoring " + dest + " from " + src);
  
      return setResultMessage("Successfully restored " + dest + " from " + src);

    } catch (IOException ex) {
      logger.error("Error attempting to restore: " + src, ex);
      throw new QueryException("Error attempting to restore: " + src, ex);
    }
  }


  /**
   * Perform the transfer with the configured datastream.
   * @return <code>null</code>, as this operation does not return a number.
   */
  protected Long doTx(Connection conn, InputStream inputStream) throws QueryException {
    conn.getSession().restore(inputStream, getDestination(), getSource());
    return 0L;
  }

}
