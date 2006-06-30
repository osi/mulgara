package org.mulgara.connection.param;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import junit.framework.Assert;

/**
 * Test utility for checking parameters bad to methods.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created 2005-04-03
 * @modified $Date: 2005/04/04 11:30:11 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ParameterTestUtil {

  public static final String NULL = null;
  public static final String EMPTY_STRING = "";
  public static final String SINGLE_SPACE = " ";

  public static void checkBadParam(Object ref, String methodName, String param) throws Exception {
    try {
      invokeMethod(ref, methodName, param);
      Assert.fail("Bad argument should have throw IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  private static void invokeMethod(Object cls, String methodName, String query) throws Exception {
    try {
      Method method = cls.getClass().getMethod(methodName, new Class[]{String.class});
      method.invoke(cls, new Object[]{query});
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) throw (RuntimeException) cause;
      throw e;
    }
  }
}
