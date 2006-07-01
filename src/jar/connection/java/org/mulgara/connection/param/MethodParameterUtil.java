package org.mulgara.connection.param;

/**
 * Utility for checking parameters to methods.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created 2005-04-04
 * @modified $Date: 2005/04/04 11:30:11 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public final class MethodParameterUtil {

  private static final ParameterChecker NULL_CHECKER = new NullChecker();
  private static final ParameterChecker EMPTY_STRING_CHECKER = new EmtpyStringChecker();

  private MethodParameterUtil() { }

  /**
   * Checks if <var>param</var> is <code>null</code> and throws an exception if it is.
   *
   * @param name The name of the parameter to check.
   * @param param The parameter to check.
   * @throws IllegalArgumentException If <car>param</var> is <code>null</code>.
   */
  public static void checkNoNulls(String name, Object param) throws IllegalArgumentException {
    if (!NULL_CHECKER.paramAllowed(param)) {
      throw new IllegalArgumentException(name + " parameter cannot be null");
    }
  }

  /**
   * Checks if <var>param</var> is <code>null</code> or the empty string and throws an exception if it is.
   *
   * @param name The name of the parameter to check.
   * @param param The parameter to check.
   * @throws IllegalArgumentException If <car>param</var> is <code>null</code> or the empty string.
   */
  public static void checkNotEmptyString(String name, String param) throws IllegalArgumentException {
    checkNoNulls(name, param);
    if (!EMPTY_STRING_CHECKER.paramAllowed(param)) {
      throw new IllegalArgumentException(name + " parameter cannot be the empty string");
    }
  }


//  private static void checkStringParam(String name, String param) {
//    checkParamForNull(name, param);
//    checkParamForEmptyString(name, param);
//  }
//
//  private static void checkParamForNull(String name, Object param) {
//    if (param == null) {
//      throw new IllegalArgumentException(name + " parameter cannot be null");
//    }
//  }
//
//  private static void checkParamForEmptyString(String name, String param) {
//    if (param.trim().length() == 0) {
//      throw new IllegalArgumentException(name + " parameter cannot be empty");
//    }
//  }


}
