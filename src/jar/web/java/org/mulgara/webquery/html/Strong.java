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

/**
 * Represents a strong element.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Strong extends HtmlElement {

  /**
   * Creates an empty strong element.
   */
  public Strong() {
    super(-1);
  }


  /**
   * Creates a strong element with embedded text.
   * @param text The text to embed.
   */
  public Strong(String text) {
    super(-1, new Text(text));
  }


  /**
   * Always returns false, indicating that formatting is never used on this element.
   * @see org.mulgara.webquery.html.HtmlElement#shouldIndent()
   */
  protected boolean shouldIndent() {
    return false;
  }


  protected String getTag() {
    return "strong";
  }

}
