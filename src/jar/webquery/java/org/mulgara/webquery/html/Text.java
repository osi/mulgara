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

package org.mulgara.webquery.html;

import java.io.PrintWriter;

import org.mulgara.util.StringUtil;

/**
 * Represents free floating text. Indent is irrelevant for this class.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Text extends HtmlElement {

  private StringBuilder buffer;

  /**
   * Creates a text element with no initial text.
   */
  public Text() {
    super(-1);
    buffer = new StringBuilder();
  }


  /**
   * Creates a text element from an arbitrary object.
   * @param obj The object to convert to text for this element.
   */
  public Text(Object obj) {
    super(-1);
    buffer = new StringBuilder(StringUtil.quoteAV(obj.toString()));
  }


  /**
   * Appends new text from an arbitrary object to this element.
   * @param obj The object to convert to text to append to this element.
   * @return The current instance.
   */
  public Text append(Object obj) {
    buffer.append(StringUtil.quoteAV(obj.toString()));
    return this;
  }


  /**
   * Not used for this implementation.
   * @see org.mulgara.webquery.html.HtmlElement#getTag()
   */
  protected String getTag() {
    return "";
  }


  /**
   * Always returns false, indicating that formatting is never used on this element.
   * @see org.mulgara.webquery.html.HtmlElement#shouldIndent()
   */
  protected boolean shouldIndent() {
    return false;
  }


  /**
   * Appends this text raw to the writer. There are no tags, or formating.
   * @see org.mulgara.webquery.html.HtmlElement#sendTo(java.io.PrintWriter)
   */
  public void sendTo(PrintWriter out) {
    out.append(buffer);
  }

}
