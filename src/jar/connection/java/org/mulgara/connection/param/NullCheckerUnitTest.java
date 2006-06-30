package org.mulgara.connection.param;

import junit.framework.TestCase;

/**
 * Unit test for {@link NullChecker}.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created Apr 4, 2005
 * @modified $Date: 2005/04/04 11:30:11 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class NullCheckerUnitTest extends TestCase {

  public void testParamAllowed() {
    assertFalse(new NullChecker().paramAllowed(ParameterTestUtil.NULL));
  }
}
