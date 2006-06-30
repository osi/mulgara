package org.mulgara.connection;

import java.net.URI;

import org.mulgara.query.rdf.Tucana;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;

/**
 * Returns query oriented connections to a Kowari server.
 *
 * @author Tom Adams
 * @version $Revision: 1.2 $
 * @created Apr 1, 2005
 * @modified $Date: 2005/04/03 05:02:06 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public final class KowariConnectionFactory {

  /**
   * Indicates that no security is enabled for this Kowari server.
   */
  // FIXME: Move this constant somewhere better so it can be used instead of null on the server also.
  // FIXME: Replace the use of this constant in all code that currently passes null as a flag for no security.
  public static final URI NULL_SECURITY_DOMAIN = URI.create(Tucana.NAMESPACE+"NO_SECURITY");

  /**
   * Returns a connection to the Kowari store through which to send iTQL queries.
   * <p>
   * Note. A new connection is returned for each call, they not pooled and are not thread safe. Clients should ensure
   * that they call close on the connection once it is no longer required.
   * </p>
   *
   * @param session The session to use to communicate to the Kowari server.
   * @param securityDomain The security domain of the Kowari server.
   * @return A connection to the Kowari store through which to issue iTQL queries.
   */
  public ItqlConnection getItqlConnection(Session session, URI securityDomain) {
    // FIXME: Use IoC to set this and mock it out in the unit test.
    return new DefaultItqlConnection(session, securityDomain);
  }
}
