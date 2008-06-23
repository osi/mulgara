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

import org.apache.log4j.Logger;
import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;

import edu.emory.mathcs.util.remote.io.RemoteOutputStream;
import edu.emory.mathcs.util.remote.io.server.impl.RemoteOutputStreamSrvImpl;

/**
 * Represents a command to back data up from a model.
 *
 * @created Aug 19, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Backup extends DataTx {
  
  /** The logger */
  static final Logger logger = Logger.getLogger(Backup.class.getName());

  /** The URI for the server. */
  private URI serverUri;

  /**
   * Creates a new Backup command.
   * @param source The data to back up.  May be a server or just a single graph.
   * @param destination The location where to back the data up.
   *        Only file URLs supported at the moment.
   */
  public Backup(URI source, URI destination, boolean locality) {
    super(source, destination, source, locality);
    if (!destination.getScheme().equals(FILE_SCHEME)) throw new IllegalArgumentException("Backups must be sent to a file");
    updateServerUri(source);
  }

  /**
   * @return The URI of the destination graph.
   */
  public URI getServerURI() {
    return serverUri;
  }


  /**
   * Perform a backup on a server.
   * @param conn The connection to talk to the server on.
   * @return The text describing the server that was backed up.
   * @throws QueryException There was an error asking the server to perform the backup.
   * @throws MalformedURLException The destination is not a valid file.
   */
  public Object execute(Connection conn) throws QueryException, MalformedURLException {
    // test if the server can do all the work, or if data needs to be streamed
    if (!isLocal()) {
      // server does all the work
      conn.getSession().backup(getSource(), getDestination());
    } else {
      // need to stream data through to an output stream
      FileOutputStream fileOutputStream = null;
      String destinationFile = this.getDestination().toURL().getPath();
      try {
        fileOutputStream = new FileOutputStream(destinationFile);
      } catch (FileNotFoundException ex) {
        throw new QueryException("File " + destinationFile + " cannot be created for backup. ", ex);
      }

      // send to open method for backing up to a stream
      backup(conn, getSource(), fileOutputStream);
    }
  
    return setResultMessage("Successfully backed up " + getSource() + " to " + getDestination() + ".");
  }


  /**
   * Public interface to perform a backup into an output stream.
   * This is callable directly, without an AST interface.
   * @param conn The connection to a server to be backed up.
   * @param source The URI describing the graph on the server to back up.
   * @param outputStream The output which will receive the data to be backed up.
   * @throws QueryException There was an error asking the server to perform the backup.
   */
  public static void backup(Connection conn, URI source, OutputStream outputStream) throws QueryException {
    // open and wrap the outputstream
    RemoteOutputStreamSrvImpl srv = new RemoteOutputStreamSrvImpl(outputStream);

    // prepare it for exporting
    try {
      UnicastRemoteObject.exportObject(srv);
    } catch (RemoteException rex) {
      throw new QueryException("Unable to backup "+ source + " to an output stream", rex);
    }

    OutputStream marshallingOutputStream = new RemoteOutputStream(srv);

    // perform the backup
    try {
      conn.getSession().backup(source, marshallingOutputStream);
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
  protected Long doTx(Connection conn, InputStream inputStream) throws QueryException {
    return null;
  }

  /**
   * Sets the server URI for this server operation.
   * @param uri The URI to determine the server URI from.
   */
  private URI updateServerUri(URI uri) {
    serverUri = calcServerUri(uri);
    return serverUri;
  }
}
