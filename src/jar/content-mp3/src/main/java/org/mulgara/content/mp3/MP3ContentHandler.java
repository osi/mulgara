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

package org.mulgara.content.mp3;

// Java 2 standard packages
// Java 2 enterprise packages
import java.net.URI;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

// Third party packages
import org.apache.log4j.Logger; // Apache Log4J

// Local packages
import org.mulgara.content.Content;
import org.mulgara.content.ContentHandler;
import org.mulgara.content.ContentHandlerException;
import org.mulgara.content.NotModifiedException;
import org.mulgara.query.TuplesException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Statements;

/**
 * Resolves constraints in models defined by MP3 ID3 tags.
 *
 * @created 2004-09-21
 *
 * @author Mark Ludlow
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:43 $ @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MP3ContentHandler implements ContentHandler {

  /** Logger. */
  @SuppressWarnings("unused")
  private static Logger logger = Logger.getLogger(MP3ContentHandler.class.getName());

  /**
   * The MIME type of RDF/XML.
   */
  private static final MimeType AUDIO_MPEG;

  static {
    try {
      AUDIO_MPEG = new MimeType("audio", "mpeg");
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Parses the ID3 tags of the MP3 file pointed to by the content object which
   * are then converted to a statements object.
   *
   * @param content The actual content we are going to be parsing
   * @param resolverSession The session in which this resolver is being used
   *
   * @return The parsed statements object
   *
   * @throws ContentHandlerException
   */
  public Statements parse(Content content, ResolverSession resolverSession)
    throws ContentHandlerException, NotModifiedException {

    // Container for our statements
    MP3Statements statements = null;

    try {

      // Attempt to create the MP3 statements
      statements = new MP3Statements(content, resolverSession);
    } catch (TuplesException tuplesException) {
      URI u = content.getURI();
      if (u == null) {
        throw new ContentHandlerException("Unable to create statements object from stream with type" +
                                          content.getContentType(), tuplesException);
      }
      throw new ContentHandlerException("Unable to create statements object from content object: " +
                                        content.getURI().toString(), tuplesException);
    }

    return statements;
  }

  /**
   * @return <code>true</code> if the file part of the URI has an
   *   <code>.mp3</code> extension
   */
  public boolean canParse(Content content) throws NotModifiedException
  {
    MimeType contentType = content.getContentType();
    if (contentType != null && AUDIO_MPEG.match(contentType)) {
      return true;
    }

    if (content.getURI() == null) {
      return false;
    }
                                                                                
    // Obtain the path part of the URI
    String path = content.getURI().getPath();
    if (path == null) {
      return false;
    }
    assert path != null;
                                                                                
    // We recognize a fixed extension
    return path.endsWith(".mp3");
  }

  /**
   * @throws ContentHandlerException  always, as MP3s aren't pure metadata
   *   stores and don't make any sense to overwrite
   */
  public void serialize(Statements      statements,
                        Content         content,
                        ResolverSession resolverSession)
    throws ContentHandlerException
  {
    throw new ContentHandlerException("MP3s are not writable.");
  }
}
