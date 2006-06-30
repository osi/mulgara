package org.mulgara.connection.param;


/**
 * Checks that nulls are not passed to methods.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created 2005-04-05
 * @modified $Date: 2005/04/04 11:30:11 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class NullChecker implements ParameterChecker {

  /**
   * Checks if this checker allows a parameter with the given value.
   *
   * @param param The parameter to check the value of.
   * @return <code>true</code> if the parameter is allowed by this checker.
   */
  public boolean paramAllowed(Object param) {
    return param != null;
  }
}
