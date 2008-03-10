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

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.rdf.XSD;
import org.mulgara.store.stringpool.*;
import org.mulgara.util.Constants;


/**
 * An SPTypedLiteral that represents xsd:decimal literals.
 * TODO This is currently implemented as a 64 bit (long) value.  xsd:decimal
 * and xsd:integer are not properly supported by this class.
 *
 * @created 2004-10-05
 *
 * @author David Makepeace
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
public final class SPDecimalImpl extends AbstractSPTypedLiteral {

  private final static Logger logger = Logger.getLogger(SPDecimalImpl.class);

  private long l;

  static final int TYPE_ID = 2; // Unique ID


  SPDecimalImpl(int subtypeId, URI typeURI, long l) {
    super(TYPE_ID, subtypeId, typeURI);
    this.l = l;
  }


  SPDecimalImpl(int subtypeId, URI typeURI, ByteBuffer data) {
    this(subtypeId, typeURI, data.getLong());
  }


  SPDecimalImpl(int subtypeId, URI typeURI, String lexicalForm) {
    this(subtypeId, typeURI, Long.parseLong(lexicalForm));
  }


  /* from SPObject interface. */

  public ByteBuffer getData() {
    ByteBuffer data = ByteBuffer.allocate(Constants.SIZEOF_LONG);
    data.putLong(l);
    data.flip();
    return data;
  }


  public SPComparator getSPComparator() {
    return SPDecimalComparator.getInstance();
  }


  public String getLexicalForm() {
    return Long.toString(l);
  }


  /* from Comparable interface. */

  public int compareTo(Object o) {
    // Compare types.
    int c = super.compareTo(o);
    if (c != 0) return c;

    // Compare the longs.
    return compare(l, ((SPDecimalImpl)o).l);
  }


  /* from Object. */

  public int hashCode() {
    return (int)(l * 7) | (int)(l >> 32);
  }


  public boolean equals(Object obj) {
    // Check for null.
    if (obj == null) return false;

    try {
      return l == ((SPDecimalImpl)obj).l;
    } catch (ClassCastException ex) {
      // obj was not an SPDecimalImpl.
      return false;
    }
  }


  static int compare(long l1, long l2) {
    return l1 < l2 ? -1 : (l1 > l2 ? 1 : 0);
  }


  /** Compares the binary representations of two SPDecimalImpl objects. */
  public static class SPDecimalComparator implements SPComparator {

    private static final SPDecimalComparator INSTANCE = new SPDecimalComparator();

    public static SPDecimalComparator getInstance() {
      return INSTANCE;
    }

    public int comparePrefix(ByteBuffer d1, ByteBuffer d2, int d2Size) {
      return 0;
    }

    public int compare(ByteBuffer d1, ByteBuffer d2) {
      return SPDecimalImpl.compare(d1.getLong(), d2.getLong());
    }

  }

}
