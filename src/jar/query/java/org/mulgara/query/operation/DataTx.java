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
import java.net.URISyntaxException;
import java.net.URL;
import java.rmi.NoSuchObjectException;
import java.rmi.server.UnicastRemoteObject;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;

import edu.emory.mathcs.util.remote.io.RemoteInputStream;
import edu.emory.mathcs.util.remote.io.server.impl.RemoteInputStreamSrvImpl;

/**
 * Represents a command to move data in or out of a model.
 *
 * @created Aug 13, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class DataTx extends ServerCommand {

  /** The logger */
  static final Logger logger = Logger.getLogger(DataTx.class.getName());

  /** String constant for the extension of gzip files. */
  private static final String GZIP_EXTENSION = ".gz";

  /** String constant for the extension of zip files. */
  private static final String ZIP_EXTENSION = ".zip";

  protected static final String FILE_SCHEME = "file";

  /** The source of data to insert. */
  private final URI source;
  
  /** The graph to load data into. */
  private final URI destination;

  /** Indicates that data is to be loaded locally from the client. */
  private final boolean local;
  
  /** A stream to enable an API to load data directly. */
  private InputStream overrideStream;

  /**
   * Create a new data transfer command for loads and restores.
   * @param source The source of data to insert.
   * @param destination The graph or server to load data into.
   */
  public DataTx(URI source, URI destination, URI serverGraphURI, boolean local) {
    super(serverGraphURI);
    // make sure that the URI given to the parent was a good one
    assert source.equals(serverGraphURI) || destination.equals(serverGraphURI);
    // test and store the parameters
    if (source == null) throw new IllegalArgumentException("Need a valid source of data");
    if (destination == null) throw new IllegalArgumentException("Need a valid destination for data");
    this.source = source;
    this.destination = destination;
    this.local = local;
    overrideStream = null;
  }


  /**
   * Allows an API to set the stream for loading, instead of getting it from the
   * source URI.
   * @param overrideStream The stream to use for loading data.
   */
  public void setOverrideStream(InputStream overrideStream) {
    this.overrideStream = overrideStream;
  }

  /**
   * @return the URI of the source data.
   */
  public URI getSource() {
    return source;
  }

  /**
   * @return the destination URI for the data.
   */
  public URI getDestination() {
    return destination;
  }


  /**
   * @return the locality flag for the data.
   */
  public boolean isLocal() {
    return local;
  }


  /**
   * Perform the transfer with the configured datastream.
   * @return The number of statements affected, or <code>null</code> if this is not relevant.
   */
  protected abstract Long doTx(Connection conn, InputStream inputStream) throws QueryException;


  /**
   * Wrap the file at the source URI in an RMI object for marshalling, and send over the connection.
   * Used by Load and Restore, but not Backup, which marshalls in the opposite direction.
   * @param conn The connection to the server.
   * @return The number of statements inserted.
   * @throws QueryException There was an error working with data at the server end.
   * @throws IOException There was an error transferring data over the network.
   */
  protected long sendMarshalledData(Connection conn, boolean compressable) throws QueryException, IOException {
    if (logger.isInfoEnabled()) logger.info("loading local resource : " + source);

    RemoteInputStreamSrvImpl srv = null;
    RemoteInputStream remoteInputStream = null;
    try {

      // is the file/stream compressed?
      InputStream inputStream;
      if (compressable) inputStream = adjustForCompression(source.toURL());
      else inputStream = (overrideStream != null) ? overrideStream : source.toURL().openStream();

      // open and wrap the inputstream
      srv = new RemoteInputStreamSrvImpl(inputStream);
      UnicastRemoteObject.exportObject(srv);
      remoteInputStream = new RemoteInputStream(srv);

      // call back to the implementing class
      return doTx(conn, remoteInputStream);

    } finally {
      // clean up the RMI object
      if (srv != null) {
        try {
          UnicastRemoteObject.unexportObject(srv, false);
        } catch (NoSuchObjectException ex) {};
      }
      try {
        if (remoteInputStream != null) remoteInputStream.close();
      } catch (Exception e) { }
    }

  }


  /**
   * Gets a stream for a file.  Determines if the stream is compressed by inspecting
   * the fileName extension.
   *
   * @return a new stream which supplies uncompressed data from the file location. 
   * @param fileLocation String The URL for the file being loaded
   * @throws IOException An error while reading from the input stream.
   * @return InputStream A new input stream which supplies uncompressed data.
   */
  private InputStream adjustForCompression(URL fileLocation) throws IOException {

    if (fileLocation == null) throw new IllegalArgumentException("File name is null");

    InputStream stream = (overrideStream == null) ? fileLocation.openStream() : overrideStream;

    // wrap the stream in a decompressor if the suffixes indicate this should happen.
    String fileName = fileLocation.toString();
    if (fileName.toLowerCase().endsWith(GZIP_EXTENSION)) {
      stream = new GZIPInputStream(stream);
    } else if (fileName.toLowerCase().endsWith(ZIP_EXTENSION)) {
      stream = new ZipInputStream(stream);
    }

    assert stream != null;
    return stream;
  }
  

  /**
   * Determine the URI to be used for a server when processing a backup.
   * @param uri Can contain the URI of a graph, or of an entire server.
   * @return The URI for the server containing the uri.
   */
  public static URI calcServerUri(URI uri) {
    URI calcUri = null;
    
    // check if backing up a graph or a server
    String fragment = uri.getFragment();
    if (fragment == null) {
      if (logger.isDebugEnabled()) logger.debug("Backup for server: " + uri);
      calcUri = uri;
    } else {
      String serverUriString = uri.toString().replaceAll("#" + fragment, "");
      try {
        calcUri = new URI(serverUriString);
      } catch (URISyntaxException e) {
        throw new Error("Unable to truncate a fragment from a valid URI");
      }
    }
    return calcUri;
  }

}
