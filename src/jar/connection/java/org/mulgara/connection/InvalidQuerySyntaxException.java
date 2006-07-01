package org.mulgara.connection;

/**
 * Indicates that the format of a query does not match the required syntax for its language.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created 2005-04-03
 * @modified $Date: 2005/04/03 05:02:06 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class InvalidQuerySyntaxException extends Exception {

  public InvalidQuerySyntaxException(String message) {
    super(message);
  }

  public InvalidQuerySyntaxException(String message, Throwable cause) {
    super(message, cause);
  }
}
