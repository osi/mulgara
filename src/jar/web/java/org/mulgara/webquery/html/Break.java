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
 * Represents a line break.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Break extends HtmlElement {

  /**
   * Creates a line break with a given indent.
   * @param indent The amount to indent by.
   */
  public Break(int indent) {
    super(indent);
  }


  /**
   * Creates a line break with no indenting.
   */
  Break() {
    this(0);
  }

  /**
   * @throws UnsupportedOperationException As sub elements are not possible for this element. 
   * @see org.mulgara.webquery.html.HtmlElement#add(org.mulgara.webquery.html.HtmlElement)
   */
  public HtmlElement add(HtmlElement elt) {
    throw new UnsupportedOperationException("Line breaks do not have sub elements.");
  }


  /**
   * @see org.mulgara.webquery.html.HtmlElement#addAttr(org.mulgara.webquery.html.HtmlElement.Attr, java.lang.String)
   */
  public HtmlElement addAttr(Attr attr, String val) {
    if (attr != Attr.ID && attr != Attr.CLASS && attr != Attr.STYLE && attr != Attr.TITLE) {
      throw new UnsupportedOperationException("Line breaks do not have attributes.");
    }
    return super.addAttr(attr, val);
  }



  protected String getTag() {
    return "br";
  }

}
