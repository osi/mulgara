package org.mulgara.connection;

import java.net.URI;

import junit.framework.TestCase;
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.server.Session;

/**
 * Unit test for {@link MulgaraConnectionFactory}.
 *
 * @author Tom Adams
 * @version $Revision: 1.2 $
 * @created Apr 1, 2005
 * @modified $Date: 2005/04/03 05:02:06 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MulgaraConnectionFactoryUnitTest extends TestCase {

  private static final MockBadSession BAD_SESSION = new MockBadSession();
  private static final URI NULL_SECURITY_DOMAIN = MulgaraConnectionFactory.NULL_SECURITY_DOMAIN;

  public void testNoSecurityConstant() {
    assertEquals(URI.create("http://tucana.org/tucana#NO_SECURITY"), MulgaraConnectionFactory.NULL_SECURITY_DOMAIN);
  }

  public void testGetItqlConnection() {
    MulgaraConnectionFactory factory = new MulgaraConnectionFactory();
    assertNotNull(factory.getItqlConnection(BAD_SESSION, NULL_SECURITY_DOMAIN));
  }

  public void testBadSessionPassthrough() {
    try {
      new MulgaraConnectionFactory().getItqlConnection(BAD_SESSION, NULL_SECURITY_DOMAIN).close();
      fail("Closing connection with bad session should have thrown exception");
    } catch (MulgaraConnectionException expected) { }
  }
}
