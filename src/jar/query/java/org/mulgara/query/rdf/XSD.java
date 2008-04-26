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

package org.mulgara.query.rdf;

// Java 2 standard packages
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.text.ParseException;

// Date utils
import com.mousepushers.date.DateParser;
import com.mousepushers.date.DateFormats;
import com.mousepushers.date.DateFormatter;

/**
 * XML Schema datatype constants.
 *
 * @see <a href="http://www.w3.org/TR/xmlschema-2/"/><cite>XML Schema Part 2:
 *   Datatypes</cite></a>
 *
 * @created 2004-03-23
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.11 $
 *
 * @modified $Date: 2005/03/02 11:21:26 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2004
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class XSD {

  /**
   * XML namespace for XML Schema datatypes:
   * <code>http://www.w3.org/2001/XMLSchema#</code>.
   *
   * Note that this isn't the correct
   * namespace from the XML Schema standard, because of the trailing
   * <code>#</code> character.  The use here is based on the examples in the
   * editor's draft of RDF Datatyping from August 19th, 2002.
   *
   * @see <a href="http://www.w3.org/TR/xmlschema-2/#namespaces"><cite>XML
   *      Schema Part 2: Datatypes</cite> &sect;3.1</a>
   */
  public final static String NAMESPACE = "http://www.w3.org/2001/XMLSchema#";

  /**
   * URI for the XML Schema <code>xsd:string</code> datatype.
   */
  public final static URI STRING_URI = URI.create(NAMESPACE + "string");

  /**
   * URI for the XML Schema <code>xsd:decimal</code> datatype.
   */
  public final static URI DECIMAL_URI = URI.create(NAMESPACE + "decimal");

  /**
   * URI for the XML Schema <code>xsd:decimal</code> datatype.
   */
  public final static URI INT_URI = URI.create(NAMESPACE + "int");

  /**
   * URI for the XML Schema <code>xsd:float</code> datatype;
   */
  public final static URI FLOAT_URI = URI.create(NAMESPACE + "float");

  /**
   * URI for the XML Schema <code>xsd:double</code> datatype;
   */
  public final static URI DOUBLE_URI = URI.create(NAMESPACE + "double");

  /**
   * URI for the XML Schema <code>xsd:date</code> datatype.
   */
  public final static URI DATE_URI = URI.create(NAMESPACE + "date");

  /**
   * URI for the XML Schema <code>xsd:dateTime</code> datatype.
   */
  public final static URI DATE_TIME_URI = URI.create(NAMESPACE + "dateTime");

  /**
   * URI for the XML Schema <code>xsd:gYearMonth</code> datatype;
   */
  public final static URI GYEARMONTH_URI = URI.create(NAMESPACE + "gYearMonth");

  /**
   * URI for the XML Schema <code>xsd:gYear</code> datatype;
   */
  public final static URI GYEAR_URI = URI.create(NAMESPACE + "gYear");

  /**
   * URI for the XML Schema <code>xsd:gMonthDay</code> datatype;
   */
  public final static URI GMONTHDAY_URI = URI.create(NAMESPACE + "gMonthDay");

  /**
   * URI for the XML Schema <code>xsd:gDay</code> datatype;
   */
  public final static URI GDAY_URI = URI.create(NAMESPACE + "gDay");

  /**
   * URI for the XML Schema <code>xsd:gMonth</code> datatype;
   */
  public final static URI GMONTH_URI = URI.create(NAMESPACE + "gMonth");

  /**
   * URI for the XML Schema <code>xsd:boolean</code> datatype;
   */
  public final static URI BOOLEAN_URI = URI.create(NAMESPACE + "boolean");

  /**
   * URI for the XML Schema <code>xsd:hexBinary</code> datatype;
   */
  public final static URI HEX_BINARY_URI = URI.create(NAMESPACE + "hexBinary");

  /**
   * URI for the XML Schema <code>xsd:base64Binary</code> datatype;
   */
  public final static URI BASE64_BINARY_URI = URI.create(NAMESPACE + "base64Binary");

  /**
   * Date format used by <code>xsd:date</code>.
   *
   * This is a highly abbreviated version of ISO 8601.
   */
  public final static String DATE_FORMAT = DateFormats.yyyy_MM_dd_format;

  /**
   * Date format used by <code>xsd:dateTime</code>.
   *
   * This is a highly abbreviated version of ISO 8601.
   */
  public final static String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

  /**
   * Date format used by <code>xsd:gYear</code>.
   */
  public final static String YEAR_FORMAT = "yyyy";


  // Set up dates for 0000 and 0001
  public static Date ONE_BCE;
  public static Date ONE_CE;

  static {
    try {
      ONE_BCE = DateParser.parse("0000", YEAR_FORMAT, Locale.getDefault());
      ONE_CE = DateParser.parse("0001", YEAR_FORMAT, Locale.getDefault());
    }
    catch (ParseException e) {
      // Should never be thrown
      throw new IllegalArgumentException("Cannot parse date");
    }
  }

  /**
   * Returns the lexical form of the XSD dateTime value according to
   * "3.2.7.2 Canonical representation" of
   * http://www.w3.org/TR/2004/REC-xmlschema-2-20041028/
   * with the following exceptions:
   * - Timezones are not supported
   * - Dates before 1 CE (i.e. 1 AD) are handled according to ISO 8601:2000
   *   Second Edition:
   *     "0000" is the lexical representation of 1 BCE
   *     "-0001" is the lexical representation of 2 BCE
   * @return the lexical form of the XSD dateTime value
   */
  public static String getLexicalForm(Date date) {
    StringBuffer lexicalForm;
    String dateTime = DateFormatter.formatDate(date, XSD.DATE_TIME_FORMAT,
        Locale.getDefault());
    int len = dateTime.length();
    if (dateTime.indexOf('.', len - 4) != -1) {
      while (dateTime.charAt(len - 1) == '0') {
        len--;
      }
      if (dateTime.charAt(len - 1) == '.') {
        len--;
      }
      lexicalForm = new StringBuffer(dateTime.substring(0, len));
    }
    else {
      lexicalForm = new StringBuffer(dateTime);
    }

    if (date.before(ONE_CE)) {
      StringBuffer year = new StringBuffer(String.valueOf(Integer.parseInt(
          DateFormatter.formatDate(date, YEAR_FORMAT, Locale.getDefault())) - 1));
      while (year.length() < 4)
          {
        year.insert(0, '0');
      }
      lexicalForm.replace(0, lexicalForm.indexOf("-", 4), year.toString());
      if (date.before(ONE_BCE)) {
        lexicalForm.insert(0, "-");
      }
    }
    return lexicalForm.toString();
  }
}
