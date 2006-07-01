package org.mulgara.connection;

import java.net.URI;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;

import org.mulgara.connection.param.ParameterTestUtil;

/**
 * Unit test for {@link DefaultItqlConnection}.
 *
 * @author Tom Adams
 * @version $Revision: 1.4 $
 * @created Apr 1, 2005
 * @modified $Date: 2005/04/04 11:30:10 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class DefaultItqlConnectionUnitTest extends TestCase {

  private static final MockBadSession BAD_SESSION = new MockBadSession();
  private static final URI NULL_SECURITY_DOMAIN = MulgaraConnectionFactory.NULL_SECURITY_DOMAIN;
  private static final String EXECUTE_UPDATE_METHOD = "executeUpdate";
  private static final String EXECUTE_QUERY_METHOD = "executeQuery";
  private static final String NULL = ParameterTestUtil.NULL;
  private static final String EMPTY_STRING = ParameterTestUtil.EMPTY_STRING;
  private static final String SINGLE_SPACE = ParameterTestUtil.SINGLE_SPACE;

  public void testClose() {
    try {
      new DefaultItqlConnection(BAD_SESSION, NULL_SECURITY_DOMAIN).close();
      fail("Bad connection should throw MulgaraConnectionException");
    } catch (MulgaraConnectionException expected) { }
  }

  public void testExecuteSimpleBadQuery() throws Exception {
    DefaultItqlConnection connection = new DefaultItqlConnection(BAD_SESSION, NULL_SECURITY_DOMAIN);
    checkBadParam(connection, EXECUTE_UPDATE_METHOD, NULL);
    checkBadParam(connection, EXECUTE_UPDATE_METHOD, EMPTY_STRING);
    checkBadParam(connection, EXECUTE_UPDATE_METHOD, SINGLE_SPACE);
  }

  public void testExecuteSimpleBadUpdate() throws Exception {
    DefaultItqlConnection connection = new DefaultItqlConnection(BAD_SESSION, NULL_SECURITY_DOMAIN);
    checkBadParam(connection, EXECUTE_QUERY_METHOD, NULL);
    checkBadParam(connection, EXECUTE_QUERY_METHOD, EMPTY_STRING);
    checkBadParam(connection, EXECUTE_QUERY_METHOD, SINGLE_SPACE);
  }

  private void checkBadParam(DefaultItqlConnection connection, String method, String param) throws Exception {
    ParameterTestUtil.checkBadParam(connection, method, param);
  }
}
