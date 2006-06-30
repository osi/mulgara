package org.mulgara.connection;

import java.net.URI;
import java.util.Set;
import java.util.List;
import java.io.OutputStream;
import java.io.InputStream;

import org.mulgara.query.Answer;
import org.mulgara.query.ModelExpression;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.rules.RulesRef;
import org.mulgara.server.Session;

/**
 * Mock {@link Session} for unit testing.
 *
 * @author Tom Adams
 * @version $Revision: 1.2 $
 * @created Apr 1, 2005
 * @modified $Date: 2005/06/26 12:47:55 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MockBadSession implements Session {

  public void insert(URI modelURI, Set statements) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void insert(URI modelURI, Query query) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void delete(URI modelURI, Set statements) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void delete(URI modelURI, Query query) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void backup(URI sourceURI, URI destinationURI) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void backup(URI sourceURI, OutputStream outputStream) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void restore(URI serverURI, URI sourceURI) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void restore(InputStream inputStream, URI serverURI, URI sourceURI) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public Answer query(Query query) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public List query(List queries) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void createModel(URI modelURI, URI modelTypeURI) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void removeModel(URI uri) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public boolean modelExists(URI uri) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public long setModel(URI uri, ModelExpression modelExpression) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public long setModel(InputStream inputStream, URI uri, ModelExpression modelExpression) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void setAutoCommit(boolean autoCommit) throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void commit() throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void rollback() throws QueryException {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void close() throws QueryException {
    throw new QueryException("Implement me...");
  }

  public boolean isLocal() {
    throw new UnsupportedOperationException("Implement me...");
  }

  public void login(URI securityDomain, String username, char[] password) {
    throw new UnsupportedOperationException("Implement me...");
  }
  
  public RulesRef buildRules(URI uri, URI uri2, URI uri3) {
    throw new UnsupportedOperationException("Implement me...");
  }
  
  public void applyRules(RulesRef rulesRef) {
    throw new UnsupportedOperationException("Implement me...");
  }
}
