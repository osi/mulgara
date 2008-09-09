/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/osl-3.0.txt
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 */

package org.mulgara.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


/**
 * Represents a data object as JSON.
 *
 * @created Jul 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class StreamedSparqlJSONObject implements StreamedAnswer {

  /** The encoded data. */
  final Object objectData;

  /** The writer used for creating the XML. */
  protected OutputStreamWriter s = null;

  /** The byte output stream used for creating the XML. */
  protected OutputStream output = null;

  /**
   * Creates an XML object encoding.
   * @param objectData The data to encode.
   * @param output Where to send the output.
   */
  public StreamedSparqlJSONObject(Object objectData, OutputStream output) {
    this.objectData = objectData;
    this.output = output;
  }

  /**
   * Put the parts of the document together, and close the stream.
   * @see org.mulgara.protocol.StreamedXMLAnswer#emit()
   */
  public void emit() throws IOException {
    s = new OutputStreamWriter(output);
    s.append("{ \"data\": ");
    s.append(jsonEscape(objectData));
    s.append(" }");
    s.flush();
  }

  /** Trivial escaping. */
  public static String jsonEscape(Object o) {
    if (o instanceof Number) return o.toString();
    String data = o.toString();
    data = data.replace("\"", "\\\"");
    data = data.replace("\\", "\\\\");
    data = data.replace("/", "\\/");
    data = data.replace("\b", "\\b");
    data = data.replace("\f", "\\f");
    data = data.replace("\n", "\\n");
    data = data.replace("\r", "\\r");
    data = data.replace("\t", "\\t");
    return data;
  }
}
