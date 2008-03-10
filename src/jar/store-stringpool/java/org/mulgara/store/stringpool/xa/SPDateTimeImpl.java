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

package org.mulgara.store.stringpool.xa;

// Java 2 standard packages
import java.net.URI;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

// Third party packages
import org.apache.log4j.Logger;

// Date utils
import com.mousepushers.date.DateParser;

// Locally written packages
import org.mulgara.query.rdf.XSD;
import org.mulgara.store.stringpool.*;
import org.mulgara.util.Constants;


/**
 * An SPObject that represents XSD dateTime values.
 *
 * @created 2004-09-27
 *
 * @author David Makepeace
 * @author Edwin Shin
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/03/11 04:15:22 $
 *
 * @maintenanceAuthor $Author: raboczi $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public final class SPDateTimeImpl extends AbstractSPTypedLiteral {

  private final static Logger logger = Logger.getLogger(SPDateTimeImpl.class);

  static final int TYPE_ID = 6; // Unique ID

  static final URI TYPE_URI = XSD.DATE_TIME_URI;

  private Date date;


  SPDateTimeImpl(Date date) {
    super(TYPE_ID, TYPE_URI);

    if (date == null) {
      throw new IllegalArgumentException("Null \"date\" parameter");
    }

    this.date = date;
  }


  SPDateTimeImpl(ByteBuffer data) {
    this(data.getLong());
  }


  SPDateTimeImpl(long l) {
    this(new Date(l));
  }


  static SPDateTimeImpl newInstance(String lexicalForm) {

    StringBuffer sb = new StringBuffer(lexicalForm);
    int pos = sb.length() - 4;
    int index = sb.indexOf(".", pos);
    if (index == -1) {
      sb.append(".000");
    }
    else {
      if (index == sb.length() - 1) {
        throw new IllegalArgumentException("Cannot parse date: " + lexicalForm);
      }
      int pad = pos - index;
      while (pad < 0) {
        sb.append("0");
        pad++;
      }
    }
    lexicalForm = sb.toString();
    try {
      Date date = DateParser.parse(lexicalForm, XSD.DATE_TIME_FORMAT,
          Locale.getDefault());
      return new SPDateTimeImpl(date);
    }
    catch (ParseException ex) {
      throw new IllegalArgumentException("Cannot parse date: " + lexicalForm);
    }

  }

  /* from SPObject interface. */

  public ByteBuffer getData() {
    ByteBuffer data = ByteBuffer.allocate(Constants.SIZEOF_LONG);
    data.putLong(date.getTime());
    data.flip();
    return data;
  }

  public SPComparator getSPComparator() {
    return SPDateTimeComparator.getInstance();
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
  public String getLexicalForm() {
    return XSD.getLexicalForm(date);
  }

  /* from Comparable interface. */

  public int compareTo(Object o) {
    // Compare types.
    int c = super.compareTo(o);
    if (c != 0) return c;

    // Compare the Dates.
    return date.compareTo(((SPDateTimeImpl)o).date);
  }


  /* from Object. */

  public int hashCode() {
    return date.hashCode();
  }


  public boolean equals(Object obj) {
    // Check for null.
    if (obj == null) return false;

    try {
      return date.equals(((SPDateTimeImpl)obj).date);
    } catch (ClassCastException ex) {
      // obj was not an SPDateTimeImpl.
      return false;
    }
  }


  /** Compares the binary representations of two SPDateTimeImpl objects. */
  public static class SPDateTimeComparator implements SPComparator {

    private static final SPDateTimeComparator INSTANCE =
        new SPDateTimeComparator();

    public static SPDateTimeComparator getInstance() {
      return INSTANCE;
    }

    public int comparePrefix(ByteBuffer d1, ByteBuffer d2, int d2Size) {
      return 0;
    }

    public int compare(ByteBuffer d1, ByteBuffer d2) {
      return compare(d1.getLong(), d2.getLong());
    }

    private static int compare(long a, long b) {
      return a == b ? 0 : (a < b ? -1 : 1);
    }

  }

}
