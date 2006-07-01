package org.mulgara.connection;

import junit.framework.TestCase;

import org.mulgara.connection.param.ParameterTestUtil;

/**
 * Unit test for {@link ItqlQueryBuilder}.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created 2005-04-03
 * @modified $Date: 2005/04/04 11:30:10 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ItqlQueryBuilderUnitTest extends TestCase {

  private static final String BUILD_QUERY_METHOD = "buildQuery";
  private static final String NULL = ParameterTestUtil.NULL;
  private static final String EMPTY_STRING = ParameterTestUtil.EMPTY_STRING;
  private static final String SINGLE_SPACE = ParameterTestUtil.SINGLE_SPACE;

  public void testBadParams() throws Exception {
    ItqlQueryBuilder builder = new ItqlQueryBuilder();
    checkBadParam(builder, NULL);
    checkBadParam(builder, EMPTY_STRING);
    checkBadParam(builder, SINGLE_SPACE);
  }

  public void tesBuildQuery() throws InvalidQuerySyntaxException {
    // FIXME: Is there any more testing we can do on the form of the query?
    String query = "select $s $p $o from <rmi://localhost/server1#> where $s $p $o ;";
    assertNotNull(new ItqlQueryBuilder().buildQuery(query));
  }

  private void checkBadParam(ItqlQueryBuilder builder, String param) throws Exception {
    ParameterTestUtil.checkBadParam(builder, BUILD_QUERY_METHOD, param);
  }
}
