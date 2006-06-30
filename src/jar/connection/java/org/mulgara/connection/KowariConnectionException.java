package org.mulgara.connection;

/**
 * Indicates that a connection error occured while connected/ing to a Kowari server.
 *
 * @author Tom Adams
 * @version $Revision: 1.2 $
 * @created 2005-03-31
 * @modified $Date: 2005/04/03 05:02:06 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class KowariConnectionException extends Exception {

  public KowariConnectionException(String message) {
    super(message);
  }

  public KowariConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
