package org.mulgara.connection;

import java.net.URI;

import junit.framework.TestCase;
import org.mulgara.query.rdf.Tucana;
import org.mulgara.server.Session;

/**
 * Unit test for {@link KowariConnectionFactory}.
 *
 * @author Tom Adams
 * @version $Revision: 1.2 $
 * @created Apr 1, 2005
 * @modified $Date: 2005/04/03 05:02:06 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class KowariConnectionFactoryUnitTest extends TestCase {

  private static final MockBadSession BAD_SESSION = new MockBadSession();
  private static final URI NULL_SECURITY_DOMAIN = KowariConnectionFactory.NULL_SECURITY_DOMAIN;

  public void testNoSecurityConstant() {
    assertEquals(URI.create("http://tucana.org/tucana#NO_SECURITY"), KowariConnectionFactory.NULL_SECURITY_DOMAIN);
  }

  public void testGetItqlConnection() {
    KowariConnectionFactory factory = new KowariConnectionFactory();
    assertNotNull(factory.getItqlConnection(BAD_SESSION, NULL_SECURITY_DOMAIN));
  }

  public void testBadSessionPassthrough() {
    try {
      new KowariConnectionFactory().getItqlConnection(BAD_SESSION, NULL_SECURITY_DOMAIN).close();
      fail("Closing connection with bad session should have thrown exception");
    } catch (KowariConnectionException expected) { }
  }
}
