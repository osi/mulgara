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

package org.mulgara.protocol.http;

import java.io.IOException;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

/**
 * This class extends multipart MIME objects to lookup parameter values.
 *
 * @created Sep 17, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class MimeMultiNamedPart extends MimeMultipart {

  /**
   * @param src The data source to retrieve the MIME data from
   * @throws MessagingException If the source cannot be parsed as valid MIME data.
   */
  public MimeMultiNamedPart(DataSource arg0) throws MessagingException {
    super(arg0);
  }


  /**
   * Goes through MIME data to look for a parameter.
   * @param paramName The name of the parameter to retrieve
   * @return The value for the parameter.
   * @throws MessageException If the MIME data could not be parsed.
   */
  public Object getParameter(String param) throws MessagingException, IOException {
    BodyPart part = getNamedPart(param);
    return part == null ? null : part.getContent();
  }


  /**
   * Goes through MIME data to look for a string parameter.
   * @param paramName The name of the parameter to retrieve
   * @return The string value for the parameter, converting if needed.
   * @throws MessageException If the MIME data could not be parsed.
   */
  public String getParameterString(String param) throws MessagingException, IOException {
    Object obj = getParameter(param);
    return obj == null ? null : obj.toString();
  }


  /**
   * Finds a body part that has the requested name.
   * @param paramName The name of the part to get.
   * @return The body part with the requested name, or null if not found.
   * @throws MessagingException If the MIME object could not be scanned.
   */
  public BodyPart getNamedPart(String paramName) throws MessagingException {
    for (int p = 0; p < getCount(); p++) {
      BodyPart bpart = getBodyPart(p);
      if (paramName.equalsIgnoreCase(getPartName(bpart))) return bpart;
    }
    return null;
  }


  /**
   * Look up the name of a part by index.
   * @param partNr The index of the part to look up.
   * @return The name of the part, or null if not available.
   * @throws MessagingException If the MIME object could not be scanned.
   */
  public String getPartName(int partNr) throws MessagingException {
    return getPartName(getBodyPart(partNr));
  }


  /**
   * Gets the name of a body part.
   * @param part The body part to get the name of.
   * @return The name of this part, or <code>null</code> if no name can be found.
   * @throws MessagingException The part could not be accessed.
   */
  public static String getPartName(BodyPart part) throws MessagingException {
    String[] cds = part.getHeader("Content-Disposition");
    if (cds == null) return null;
    // probably only has one Content-Disposition header, but check all anyway
    for (String header: cds) {
      for (String kv: header.split("; ")) {
        int eq = kv.indexOf('=');
        if (eq >= 0) {
          // a key=value element
          String key = kv.substring(0, eq);
          if ("name".equalsIgnoreCase(key)) {
            String value = kv.substring(eq + 1);
            return stripQuotes(value);
          }
        }
      }
    }
    return null;
  }


  /**
   * Removes quote characters from around a string.
   * @param str The string to remove quotes from.
   * @return The part of str that was between quotes, or all of str if there were no quotes.
   */
  private static String stripQuotes(String str) {
    int l = str.length() - 1;
    if (str.charAt(0) == '"' && str.charAt(l) == '"') str = str.substring(1, l);
    return str;
  }

}
