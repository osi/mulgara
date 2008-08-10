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
 * Represents a div element. This implementation always uses an id attribute.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Div extends HtmlElement {

  /**
   * Creates a div with a given indent.
   * @param indent The amount to indent by.
   * @param id The name of the id for the div.
   */
  public Div(int indent, String id) {
    super(indent);
    this.addAttr(Attr.CLASS, id);
  }


  /**
   * Creates a div with no indenting.
   * @param id The name of the id for the div.
   */
  Div(String id) {
    this(0, id);
  }


  /**
   * Creates a div with a given indent, and a list of sub elements.
   * @param indent The amount to indent by.
   * @param id The name of the id for the div.
   * @param subElements a list of sub elements inside this div.
   */
  Div(int indent, String id, HtmlElement... subElements) {
    super(indent, subElements);
    this.addAttr(Attr.CLASS, id);
  }


  /**
   * Creates a div with no initial indenting, and a list of sub elements.
   * @param id The name of the id for the div.
   * @param subElements a list of sub elements inside this div.
   */
  Div(String id, HtmlElement... subElements) {
    this(0, id, subElements);
  }


  /** @see org.mulgara.webquery.html.HtmlElement#getTag() */
  protected String getTag() {
    return "div";
  }

}
