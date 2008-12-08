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
import org.mulgara.query.GraphResource;
import org.mulgara.query.QueryException;
import org.mulgara.query.rdf.Mulgara;

/**
 * Represents a command to load data into a model.
 *
 * @created Aug 19, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Load extends DataInputTx {

  /** The logger */
  static final Logger logger = Logger.getLogger(Load.class.getName());
  
  /** Dummy source model URI to pass to the server when overriding with local stream. */
  protected static final URI DUMMY_RDF_SOURCE = URI.create(Mulgara.NAMESPACE+"locally-sourced-inputStream.rdf");
  
  /** Graph resource form of the source URI */
  private final GraphResource srcRsc;
  
  /**
   * Build a load operation, loading data from one URI into a graph specified by another URI.
   * @param source The URI of the source of the RDF data.
   * @param graphURI The URI of the graph to receive the data.
   * @param local Set to <code>true</code> to indicate that the source is on the client system.
   */
  public Load(URI source, URI graphURI, boolean local) {
    super(source, graphURI, graphURI, local);
    
    // Validate arguments.
    if (graphURI == null) throw new IllegalArgumentException("Need a valid destination graph URI");
    
    srcRsc = new GraphResource(source == null ? DUMMY_RDF_SOURCE : source);
  }


  /**
   * Alternate constructor for creating a load operation whose source will be a local input stream.
   * @param graphURI The URI of the graph to receive the data.
   * @param stream The local input stream that is the source of data to load.
   */
  public Load(URI graphURI, InputStream stream) {
    this(null, graphURI, true);
    setOverrideInputStream(stream);
  }


  /**
   * Load the data into the destination graph through the given connection.
   * @param conn The connection to load the data over.
   * @return The number of statements that were inserted.
   */
  public Object execute(Connection conn) throws QueryException {
    URI src = getSource();
    URI dest = getDestination();

    if (isLocal() && !conn.isRemote() && overrideInputStream == null) {
      logger.error("Used a LOCAL modifier when loading <" + src + "> to <" + dest + "> on a non-remote server.");
      throw new QueryException("LOCAL modifier is not valid for LOAD command when not using a client-server connection.");
    }

    try {
      long stmtCount = isLocal() ? sendMarshalledData(conn, true) : conn.getSession().setModel(dest, srcRsc);
      if (logger.isDebugEnabled()) logger.debug("Loaded " + stmtCount + " statements from " + src + " into " + dest);
  
      if (stmtCount > 0L) setResultMessage("Successfully loaded " + stmtCount + " statements from " + 
          (src != null ? src : "input stream") + " into " + dest);
      else setResultMessage("WARNING: No valid RDF statements found in " + (src != null ? src : "input stream"));
      
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
  @Override
  protected Long doTx(Connection conn, InputStream inputStream) throws QueryException {
    return conn.getSession().setModel(inputStream, getDestination(), srcRsc);
  }


  /**
   * Get the text of the command, or generate a virtual command if no text was parsed.
   * @return The query that created this command, or a generated query if no query exists.
   */
  public String getText() {
    String text = super.getText();
    if (text == null || text.length() == 0) text = "load <" + getSource() + "> into <" + getDestination() + ">";
    return text;
  }

}
