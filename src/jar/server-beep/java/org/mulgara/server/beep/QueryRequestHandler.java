package org.mulgara.server.beep;

// Java 2 standard packages
import java.io.*;

// Third party packages
import org.apache.log4j.Category;    // Log4J
import org.beepcore.beep.core.*;     // BEEP

// Local packages
import org.mulgara.query.Answer;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.sparql.protocol.StreamAnswer;

/**
* The BEEP-based ITQL application protocol profile.
*
* @author <a href="http://staff.tucanatech.com/raboczi/">Simon Raboczi</a>
* @version $Revision: 1.2 $
*/
public class QueryRequestHandler implements RequestHandler
{
  /**
  * Logger.
  */
  private final Category logger = Category.getInstance(getClass().getName());

  /**
  * The Mulgara session wrapped by this BEEP session.
  */
  private final org.mulgara.server.Session session;

  //
  // Constructor
  //

  /**
  * Construct a BEEP ITQL session that wraps a Mulgara session.
  *
  * @param session  the Mulgara session to wrap
  * @throws IllegalArgumentException if <var>session</var> is <code>null</code>
  */
  QueryRequestHandler(org.mulgara.server.Session session)
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
    // Extract the query from the message
    Query query;
    try {
      ObjectInputStream ois =
        new ObjectInputStream(message.getDataStream().getInputStream());
      query = (Query) ois.readObject();
      ois.close();
    }
    catch (ClassNotFoundException e) {
      replyERR(message,
               BEEPError.CODE_PARAMETER_ERROR,    // error code
               "Couldn't deserialize class", e);  // error message
      return;
    }
    catch (IOException e) {
      replyERR(message,
               BEEPError.CODE_PARAMETER_ERROR,  // error code
               "Couldn't read Query", e);       // error message
      return;
    }
    assert query != null;

    logger.info("Received query: "+query);

    // Evaluate the query
    Answer answer;
    try {
      answer = session.query(query);
    }
    catch (QueryException e) {
      replyERR(message,
               BEEPError.CODE_REQUESTED_ACTION_ABORTED,  // error code
               "Couldn't answer query", e);              // error message
      return;
    }

    logger.info("Evaluated answer: "+answer);

    // Send the reply
    OutputDataStream outputDataStream =
      new OutputDataStream(new MimeHeaders("application/rdf+xml"));
    try {
      message.sendRPY(outputDataStream);
      StreamAnswer.serialize(answer,
                             new OutputDataStreamAdaptor(outputDataStream));
      outputDataStream.setComplete();
    }
    catch (Exception e) {
      logger.error("Error sending RPY", e);
      replyERR(message,
               BEEPError.CODE_REQUESTED_ACTION_ABORTED,  // error code
               "Error sending RPY", e);                  // error message
      return;
    }
  }

  //
  // Additional methods
  //

  /**
  * Accessor for the wrapped Mulgara session.
  *
  * @return the wrapped Mulgara session, never <code>null</code>
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
  * @param cause  the underlying exception thrown by this error condition, or
  *   <code>null</code>
  */
  private void replyERR(MessageMSG message,
                        int        errorCode,
                        String     errorMessage,
                        Throwable  cause)
  {
    assert message != null;
    assert errorMessage != null;

    logger.error("Error reply to BEEP channel: "+errorMessage, cause);

    // Create the error message
    StringWriter stringWriter = new StringWriter();
    PrintWriter  printWriter  = new PrintWriter(stringWriter);
    printWriter.print(errorMessage);

    // Add the exception message and stack trace if there's a cause
    if (cause != null) {
      if (cause.getMessage() != null) {
        printWriter.print(": "+cause.getMessage());
      }
      printWriter.println();
      cause.printStackTrace(printWriter);
    }

    printWriter.close();

    try {
      message.sendERR(errorCode, stringWriter.toString());
    }
    catch (BEEPException e) {
      message.getChannel().getSession().terminate(e.getMessage());
    }
  }
}
