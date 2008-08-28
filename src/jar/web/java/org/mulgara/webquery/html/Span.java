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
 * Represents a span element. This implementation always uses a class attribute.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Span extends HtmlElement {

  /**
   * Creates a span with a given indent.
   * @param indent The amount to indent by.
   * @param spanClass The name of the class for the span.
   */
  public Span(int indent, String spanClass) {
    super(indent);
    this.addAttr(Attr.CLASS, spanClass);
  }


  /**
   * Creates a span with no indenting.
   * @param spanClass The name of the class for the span.
   */
  public Span(String spanClass) {
    this(-1, spanClass);
  }


  /**
   * Creates a span with a given indent, and a list of sub elements.
   * @param indent The amount to indent by.
   * @param spanClass The name of the class for the span.
   * @param subElements a list of sub elements inside this span.
   */
  public Span(int indent, String spanClass, HtmlElement... subElements) {
    super(indent, subElements);
    this.addAttr(Attr.CLASS, spanClass);
  }


  /**
   * Creates a span with no initial indenting, and a list of sub elements.
   * @param spanClass The name of the class for the span.
   * @param subElements a list of sub elements inside this span.
   */
  public Span(String spanClass, HtmlElement... subElements) {
    this(-1, spanClass, subElements);
  }


  /**
   * Creates a span with a given indent, and a list of sub elements.
   * @param indent The amount to indent by.
   * @param spanClass The name of the class for the span.
   * @param test The text for a Text subElement.
   */
  public Span(int indent, String spanClass, String text) {
    super(indent, new Text(text));
    this.addAttr(Attr.CLASS, spanClass);
  }


  /**
   * Creates a span with no initial indenting, and a list of sub elements.
   * @param spanClass The name of the class for the span.
   * @param test The text for a Text subElement.
   */
  public Span(String spanClass, String text) {
    this(-1, spanClass, new Text(text));
  }


  protected String getTag() {
    return "span";
  }

}
