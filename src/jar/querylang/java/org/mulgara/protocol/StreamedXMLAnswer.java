package org.mulgara.protocol;

import java.io.IOException;
import java.nio.charset.Charset;

import org.mulgara.query.TuplesException;

/**
 * Represents an Answer that can be emitted as XML to a stream.
 *
 * @created Jul 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface StreamedXMLAnswer extends XMLAnswer, StreamedAnswer {

  /**
   * Converts the Answer to an XML String and send to output.
   * @throws TuplesException Indicates an error accessing the Answer.
   */
  public void emit() throws TuplesException, IOException;

  /**
   * Sets the character encoding when writing XML text to a byte stream.
   * @param encoding The encoding to use.
   */
  public void setCharacterEncoding(String encoding);

  /**
   * Sets the character encoding when writing XML text to a byte stream.
   * @param encoding The charset encoding to use.
   */
  public void setCharacterEncoding(Charset charset);
}