package org.mulgara.connection;

/**
 * Indicates that a connection error occured while connected/ing to a Mulgara server.
 *
 * @author Tom Adams
 * @version $Revision: 1.2 $
 * @created 2005-03-31
 * @modified $Date: 2005/04/03 05:02:06 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MulgaraConnectionException extends Exception {

  public MulgaraConnectionException(String message) {
    super(message);
  }

  public MulgaraConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
