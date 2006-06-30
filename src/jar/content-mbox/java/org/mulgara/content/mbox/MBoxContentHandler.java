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

package org.mulgara.content.mbox;

// Java 2 standard packages
import java.io.*;
import java.net.URI;
import java.util.Map;

// Java 2 enterprise packages
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

// Third party packages
import org.apache.log4j.Logger; // Apache Log4J

// Local packages
import org.mulgara.content.Content;
import org.mulgara.content.ContentHandler;
import org.mulgara.content.ContentHandlerException;
import org.mulgara.content.NotModifiedException;
import org.mulgara.content.mbox.parser.model.exception.InvalidMBoxException;
import org.mulgara.query.TuplesException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Statements;

/**
 * Resolves constraints in models defined by mbox files.
 *
 * @created 2004-10-11
 *
 * @author Mark Ludlow
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:39 $ @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MBoxContentHandler implements ContentHandler {

  /** Logger. */
  private static Logger log = Logger.getLogger(MBoxContentHandler.class);

  /**
   * The MIME type of MBoxes.
   */
  private static final MimeType TEXT_MAILBOX;

  static {

    try {

      // Create the mime type to represent mailboxes
      TEXT_MAILBOX = new MimeType("text", "mailbox");
    } catch (MimeTypeParseException e) {

      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Parses the messages of an mbox file pointed to by the content object which
   * are then converted to a statements object.
   *
   * @param content The actual content we are going to be parsing
   * @param resolverSession The session in which this resolver is being used
   *
   * @return The parsed statements object
   *
   * @throws ContentHandlerException
   */
  public Statements parse(Content content, ResolverSession resolverSession) throws
      ContentHandlerException, NotModifiedException {

    // Container for our statements
    MBoxStatements statements = null;

    try {

      // Attempt to create the MBox statements
      statements = new MBoxStatements(content, resolverSession);
    } catch (TuplesException tuplesException) {

      throw new ContentHandlerException("Unable to create statements object from " +
                                        "content object: " + content.getURI().toString(),
                                        tuplesException);
    }

    return statements;
  }

  /**
   * @return <code>true</code> if the file part of the URI has a valid mbox
   *         format (ie The file starts with "from ")
   */
  public boolean canParse(Content content) throws NotModifiedException {

    if (content.getClass().getName().equals("org.kowari.resolver.StreamContent")) {

      log.info("Unable to parse streaming content in mbox content handler.");

      return false;
    }


    // Get the mime type of the content object
    MimeType contentType = content.getContentType();

    //if (contentType != null && TEXT_MAILBOX.match(contentType)) {

      // If the mime type matches an mbox then check the first line to see if it
      // is a valid format

      // Container for our input stream from the mbox file
      InputStream inputStream = null;

      try {

        // Obtain the input stream to our file
        inputStream = content.newInputStream();
      } catch (NotModifiedException e) {
        // Not only CAN we parse this, we already have
        return true;
      } catch (IOException ioException) {

        // If we can't obtain an input stream, chances are our mbox file has
        // problems and is most likely invalid and non-parsable
        return false;
      }

      try {

        // Attempt to validate the inputStream to the file in order to validate
        // that we are working with an MBox
        validate(inputStream, content.getURI());
      } catch (InvalidMBoxException invalidMBoxException) {

        // If the mbox is in an invalid format then we can't use it so state
        // that we can't parse it
        return false;
      }

      return true;
    //}
  }

  /**
   * @throws ContentHandlerException  always, as mboxes aren't pure metadata
   *   stores and don't make any sense to overwrite
   */
  public void serialize(Statements      statements,
                        Content         content,
                        ResolverSession resolverSession)
    throws ContentHandlerException
  {
    throw new ContentHandlerException("Mboxes are not writable.");
  }

  //
  // Internal methods
  //

  /**
   * Validates the given input stream and determines whether it is a proper mbox
   * or not.
   *
   * @param stream The stream to validate
   *
   * @throws InvalidMBoxException
   */
  private void validate(InputStream stream, URI uri) throws InvalidMBoxException {

    if (stream == null) {

      // The mbox file cannot be null
      throw new InvalidMBoxException("Cannot parse null mbox objects.");
    }

    // Create an input stream reader
    InputStreamReader inputReader = new InputStreamReader(stream);

    // Create a buffered reader to read our file
    BufferedReader reader = new BufferedReader(inputReader);

    // Container for our line of the file
    String line = "";

    try {

      while (line != null && line.equals("")) {

        // Get the first line of text from the mbox file
        line = reader.readLine();
      }
    } catch (IOException ioException) {

      // We could not read the mbox file
      throw new InvalidMBoxException("MBox file [" + uri.toString() +
                                     "] was not able to be read from.",
                                     ioException);
    }

    if (!line.toLowerCase().startsWith("from ")) {

      // The mbox is not RFC822 compliant
      throw new InvalidMBoxException("MBox file [" + uri.toString() +
                                     "] was not a valid RFC822 mbox.");
    } else {

      try {

        // Get the next line of text (if any)
        line = reader.readLine();
      } catch (IOException ioException) {

        // We could not read the mbox file
        throw new InvalidMBoxException("MBox file [" + uri.toString() +
                                       "] was not able to be read from.",
                                       ioException);
      }
      if (line != null && line.length() > 0 &&
          !line.split(" ")[0].endsWith(":")) {

        // The mbox is not RFC822 compliant if the next line is not a header
      throw new InvalidMBoxException("MBox file [" + uri.toString() +
                                     "] was not a valid RFC822 mbox.");
      }
    }

    try {

      reader.close();
    } catch (IOException ioException) {

      // Since we are validating and there is not much we can do about an
      // unclosable file reader we just ignore it
    }
  }
}
