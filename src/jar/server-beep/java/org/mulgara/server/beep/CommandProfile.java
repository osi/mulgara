package org.mulgara.server.beep;

// Java 2 standard packages
import java.io.*;

// Third party packages
import org.apache.log4j.Logger;    // Log4J
import org.beepcore.beep.core.*;     // BEEP
import org.beepcore.beep.profile.*;

// Local packages
import org.mulgara.query.Answer;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.server.SessionFactory;

/**
* The BEEP-based ITQL application protocol profile.
*
* @author <a href="http://staff.tucanatech.com/raboczi/">Simon Raboczi</a>
* @version $Revision: 1.1 $
*/
public class CommandProfile implements Profile, StartChannelListener
{
  /**
  * The URI identifier for this profile.
  */
  public static final String URI = "http://mulgara.org/profiles/ITQL/COMMAND";

  /**
  * Logger.
  */
  private final static Logger logger = Logger.getLogger(CommandProfile.class);

  /**
  * The Mulgara session wrapped by this BEEP session.
  */
  private final SessionFactory sessionFactory;

  //
  // Constructor
  //

  /**
  * Construct an ITQL profile.
  *
  * @param sessionFactory  the source of Mulgara sessions for clients connecting
  *   with BEEP sessions using this profile
  * @throws IllegalArgumentException if <var>sessionFactorr</var> is
  *   <code>null</code>
  */
  public CommandProfile(SessionFactory sessionFactory)
  {
    // Validate "sessionFactory" parameter
    if (sessionFactory == null) {
      throw new IllegalArgumentException("Null \"sessionFactory\" parameter");
    }

    // Initialize fields
    this.sessionFactory = sessionFactory;
  }

  //
  // Methods implementing Profile
  //

  public StartChannelListener init(String uri, ProfileConfiguration config)
    throws BEEPException
  {
    return this;
  }

  //
  // Methods implementing StartChannelListener
  //

  /**
  * @throws StartChannelException if the Mulgara {@link #sessionFactory} fails
  *   to generate a new Mulgara session
  */
  public void startChannel(Channel channel, String encoding, String data)
    throws StartChannelException
  {
    if (logger.isDebugEnabled()) {
      logger.debug(
       "Starting channel "+channel+", encoding="+encoding+", data="+data
      );
    }

    try {
      channel.setRequestHandler(
        new QueryRequestHandler(sessionFactory.newSession())
      );
    }
    catch (QueryException e) {
      throw new StartChannelException(
        BEEPError.CODE_REQUESTED_ACTION_NOT_TAKEN,  // error code
        e.getMessage()                              // error message
      );
    }
  }

  /**
  * @param channel {@inheritDoc}
  * @throws CloseChannelException  if the underlying Mulgara session can't be
  *   closed
  */
  public void closeChannel(Channel channel) throws CloseChannelException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Closing channel "+channel);
    }

    try {
      ((QueryRequestHandler) channel.getRequestHandler()).getSession().close();
    }
    catch (QueryException e) {
      throw new CloseChannelException(
        BEEPError.CODE_REQUESTED_ACTION_NOT_TAKEN,  // error code
        e.getMessage()                              // error message
      );
    }

    channel.setRequestHandler(null);
  }

  /**
  * @return <code>true</code>
  */
  public boolean advertiseProfile(Session session)
  {
    return true;
  }
}
