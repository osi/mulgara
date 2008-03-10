/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is the Kowari Metadata Store.
 *
 * The Initial Developer of the Original Code is Plugged In Software Pty
 * Ltd (http://www.pisoftware.com, mailto:info@pisoftware.com). Portions
 * created by Plugged In Software Pty Ltd are Copyright (C) 2001,2002
 * Plugged In Software Pty Ltd. All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.server.beep;

// Java 2 standard packages
import java.io.*;

// Third party packages
import org.apache.log4j.Logger;             // Apache Log4J
import org.beepcore.beep.core.BEEPException;  // BEEP
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.Message;

// Local packages
import org.mulgara.query.Answer;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.sparql.protocol.StreamAnswer;
import org.mulgara.sparql.protocol.StreamFormatException;

/**
* An answer backed by a BEEP reply (RPY) message.
*
* This class closes the BEEP channel when it itself is closed.
*
* @created 2004-03-21
* @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
* @version $Revision: 1.9 $
* @modified $Date: 2005/01/05 04:58:59 $ by $Author: newmana $
* @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
* @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
*      Software Pty Ltd</a>
* @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
*/
class BEEPAnswer extends StreamAnswer
{
  /**
  * Logger.
  */
  private static final Logger logger = Logger.getLogger(BEEPAnswer.class);

  /**
  * The BEEP channel to close once this instance is closed.
  */
  private final Channel channel;

  //
  // Constructor
  //

  /**
  * Construct an answer based upon a BEEP reply.
  *
  * This class assumes responsibility for closing the BEEP channel associated
  * with the BEEP message passed to it.  If the message is a normal BEEP reply
  * (RPY) then the channel will be closed when the {@link #close} method is
  * called.  If the message is a BEEP error (ERR) then the channel is closed
  * as a side effect when the {@link StreamFormatException} is thrown.  The
  * only way the channel doesn't get closed is if the <var>message</var>
  * parameter is <code>null</code> and no channel exists to close.
  *
  * @param message  a BEEP reply containing an XML-formatted {@link Answer}
  * @throws IllegalArgumentException if the <var>message</var> is
  *   <code>null</code> or isn't a BEEP reply (RPY)
  * @throws StreamFormatException  if the <var>message</var> is BEEP reply, but
  *   is misformatted
  */
  BEEPAnswer(Message message) throws StreamFormatException
  {
    super(toInputStream(message));

    // Remember which channel we need to close
    channel = message.getChannel();
  }

  /**
  * @param message  a BEEP reply containing an XML-formatted {@link Answer}
  * @return the payload of the message as a stream
  * @throws IllegalArgumentException if the <var>message</var> is
  *   <code>null</code> or isn't a BEEP reply (RPY)
  */
  private static InputStream toInputStream(Message message)
  {
    // Validate "message" parameter
    if (message == null) {
      throw new IllegalArgumentException("Null \"message\" parameter");
    }
    if (message.getMessageType() != Message.MESSAGE_TYPE_RPY) {
      throw new IllegalArgumentException("Message isn't a BEEP reply (RPY)");
    }

    // Return the payload of the message as a stream
    return message.getDataStream().getInputStream();
  }

  //
  // Methods overriding StreamAnswer
  //

  /**
  * Free resources associated with this instance.
  *
  * @throws TuplesException EXCEPTION TO DO
  */
  public void close() throws TuplesException
  {
    super.close();

    try {
      channel.close();
    }
    catch (BEEPException e) {
      throw new TuplesException("Unable to close BEEP channel", e);
    }
  }
}
