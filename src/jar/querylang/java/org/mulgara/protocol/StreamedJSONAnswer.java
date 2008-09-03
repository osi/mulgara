package org.mulgara.protocol;

import java.io.IOException;

import org.mulgara.query.TuplesException;

/**
 * Represents an Answer that can be emitted as JSON to a stream.
 *
 * @created Sept 1, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface StreamedJSONAnswer extends StreamedAnswer {

  /**
   * Converts the Answer to a JSON String and send to output.
   * @throws TuplesException Indicates an error accessing the Answer.
   */
  public void emit() throws TuplesException, IOException;

}