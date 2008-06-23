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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;

import edu.emory.mathcs.util.remote.io.RemoteOutputStream;
import edu.emory.mathcs.util.remote.io.server.impl.RemoteOutputStreamSrvImpl;

/**
 * Represents a command to export data from a graph.
 *
 * @created Jun 23, 2008
 * @author Alex Hall
 * @copyright &copy; 2008 <a href="http://www.revelytix.com">Revelytix, Inc.</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Export extends DataTx {

  /**
   * Creates a new Export command.
   * @param source The graph to export.
   * @param destination The location where to export the data.
   *        Only file URLs supported at the moment.
   */
  public Export(URI source, URI destination, boolean locality) {
    super(source, destination, source, locality);
    if (!destination.getScheme().equals(FILE_SCHEME)) throw new IllegalArgumentException("Exports must be sent to a file");
  }
  
  /**
   * Perform an export on a graph.
   * @param conn The connection to talk to the server on.
   * @return The text describing the graph that was exported.
   * @throws QueryException There was an error asking the server to perform the export.
   * @throws MalformedURLException The destination is not a valid file.
   */
  public Object execute(Connection conn) throws Exception {
    // test if the server can do all the work, or if data needs to be streamed
    if (!isLocal()) {
      // server does all the work
      conn.getSession().export(getSource(), getDestination());
    } else {
      // need to stream data through to an output stream
      FileOutputStream fileOutputStream = null;
      String destinationFile = this.getDestination().toURL().getPath();
      try {
        fileOutputStream = new FileOutputStream(destinationFile);
      } catch (FileNotFoundException ex) {
        throw new QueryException("File " + destinationFile + " cannot be created for export. ", ex);
      }

      // send to open method for exporting to a stream
      export(conn, getSource(), fileOutputStream);
    }
  
    return setResultMessage("Successfully exported " + getSource() + " to " + getDestination() + ".");
  }
  
  /**
   * Public interface to perform an export into an output stream.
   * This is callable directly, without an AST interface.
   * @param conn The connection to a server to perform the export.
   * @param source The URI describing the graph on the server to export.
   * @param outputStream The output which will receive the data to be exported.
   * @throws QueryException There was an error asking the server to perform the export.
   */
  public static void export(Connection conn, URI source, OutputStream outputStream) throws QueryException {
    // open and wrap the outputstream
    RemoteOutputStreamSrvImpl srv = new RemoteOutputStreamSrvImpl(outputStream);

    // prepare it for exporting
    try {
      UnicastRemoteObject.exportObject(srv);
    } catch (RemoteException rex) {
      throw new QueryException("Unable to export "+ source + " to an output stream", rex);
    }

    OutputStream marshallingOutputStream = new RemoteOutputStream(srv);

    // perform the export
    try {
      conn.getSession().export(source, marshallingOutputStream);
    } finally {
      // cleanup the output
      if (marshallingOutputStream != null) {
        try {
          marshallingOutputStream.close();
        } catch (IOException ioe ) { /* ignore */ }
      }
      // cleanup the RMI for the output stream
      if (srv != null) {
        try {
          UnicastRemoteObject.unexportObject(srv, false);
        } catch (NoSuchObjectException ex) {};
      }
      try {
        srv.close();
      } catch (IOException e) {}
    }
  }

  /**
   * Perform the transfer with the configured datastream.
   * @return The number of statements affected, or <code>null</code> if this is not relevant.
   */
  @Override
  protected Long doTx(Connection conn, InputStream inputStream) throws QueryException {
    return null;
  }

}
