package org.mulgara.server.beep;

// Java 2 standard packages
import java.io.*;
import java.util.StringTokenizer;

// Third party packages
import org.apache.log4j.Category;    // Log4J
import org.beepcore.beep.core.*;     // BEEP

// Local packages
import org.mulgara.query.QueryException;

/**
* The BEEP-based ITQL application protocol profile.
*
* @author <a href="http://staff.tucanatech.com/raboczi/">Simon Raboczi</a>
* @version $Revision: 1.1 $
*/
public class CommandRequestHandler implements RequestHandler
{
  /**
  * Logger.
  */
  private final Category logger = Category.getInstance(getClass().getName());

  /**
  * The Kowari session wrapped by this BEEP session.
  */
  private final org.mulgara.server.Session session;

  //
  // Constructor
  //

  /**
  * Construct a BEEP ITQL session that wraps a Kowari session.
  *
  * @param session  the Kowari session to wrap
  * @throws IllegalArgumentException if <var>session</var> is <code>null</code>
  */
  CommandRequestHandler(org.mulgara.server.Session session)
  {
    // Validate "session" parameter
    if (session == null) {
      throw new IllegalArgumentException("Null \"session\" parameter");
    }

    // Initialize wrapped field
    this.session = session;
  }

  //
  // Methods implementing RequestHandler
  //

  public void receiveMSG(MessageMSG message)
  {
    // Extract the command from the message
    String command;
    try {
      command = (new BufferedReader(new InputStreamReader(
                  message.getDataStream().getInputStream()
                ))).readLine();
    }
    catch (IOException e) {
      replyERR(message,
               BEEPError.CODE_PARAMETER_ERROR,  // error code
               e.getMessage());                 // error message
      return;
    }
    assert command != null;

    logger.info("Received command: "+command);
    StringTokenizer stringTokenizer = new StringTokenizer(command);

    if (!stringTokenizer.hasMoreTokens()) {
      replyERR(message,
               BEEPError.CODE_PARAMETER_INVALID,  // error code
               "Empty command string");           // error message
    }

    String verb = stringTokenizer.nextToken();

    try {
      if ("autocommit".equals(verb)) {
        // Make sure we have a parameter token
        if (!stringTokenizer.hasMoreTokens()) {
          replyERR(message,
                   BEEPError.CODE_PARAMETER_INVALID,         // error code
                   "Autocommit command needs a parameter");  // error message
        }

        // Set autocommit as requested
        session.setAutoCommit(Boolean.getBoolean(stringTokenizer.nextToken()));

        // Send a NUL reply
        MessageStatus messageStatus = message.sendNUL();
        if (messageStatus.getMessageStatus() !=
            MessageStatus.MESSAGE_STATUS_SENT)
        {
          logger.warn(
            "Reply to autocommit returned bad status: "+messageStatus
          );
        }
      }
      else {
        replyERR(message,
                 BEEPError.CODE_PARAMETER_INVALID,   // error code
                 "Unrecognized command: "+verb);  // error message
      }
    }
    catch (BEEPException e) {
      replyERR(message,
               BEEPError.CODE_REQUESTED_ACTION_ABORTED,  // error code
               "Error sending RPY: "+e.getMessage());    // error message
    }
    catch (QueryException e) {
      replyERR(message,
               BEEPError.CODE_REQUESTED_ACTION_ABORTED,       // error code
               "Couldn't perform command: "+e.getMessage());  // error message
    }
  }

  //
  // Additional methods
  //

  /**
  * Accessor for the wrapped Kowari session.
  *
  * @return the wrapped Kowari session, never <code>null</code>
  */
  public org.mulgara.server.Session getSession()
  {
    return session;
  }

  /**
  * Send an error in response to a BEEP message, or die trying.
  *
  * This method terminates the <var>message</var>'s BEEP session when it fails
  * rather than throwing an exception.
  *
  * @param message  the BEEP message to reply to
  * @param errorCode  the BEEP error code, which should be one of the constant
  *   specified by {@link BEEPError}
  * @param errorMessage  the human-readable error message
  */
  private void replyERR(MessageMSG message, int errorCode, String errorMessage)
  {
    assert message != null;
    assert errorMessage != null;

    try {
      message.sendERR(errorCode, errorMessage);
    }
    catch (BEEPException e) {
      message.getChannel().getSession().terminate(e.getMessage());
    }
  }
}
