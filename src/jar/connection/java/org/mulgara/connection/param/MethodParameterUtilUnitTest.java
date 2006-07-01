package org.mulgara.connection.param;

import junit.framework.TestCase;

/**
 * Unit test for {@link MethodParameterUtil}.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created Apr 4, 2005
 * @modified $Date: 2005/04/04 11:30:11 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MethodParameterUtilUnitTest extends TestCase {

  private static final String NULL = ParameterTestUtil.NULL;
  private static final String EMPTY_STRING = ParameterTestUtil.EMPTY_STRING;
  private static final String SINGLE_SPACE = ParameterTestUtil.SINGLE_SPACE;
  private static final String DUMMY_PARAM_NAME = "foo";

  public void testNoNullsAllowed() {
    try {
      MethodParameterUtil.checkNoNulls(DUMMY_PARAM_NAME, NULL);
      fail("Nulls should not be allowed");
    } catch (IllegalArgumentException expected) { }
  }

  public void testEmptyStringNotAllowed() {
    checkString(NULL);
    checkString(EMPTY_STRING);
    checkString(SINGLE_SPACE);
  }

  private void checkString(String param) {
    try {
      MethodParameterUtil.checkNotEmptyString(DUMMY_PARAM_NAME, param);
      fail("Empty strings should not be allowed");
    } catch (IllegalArgumentException expected1) { }
  }
}
