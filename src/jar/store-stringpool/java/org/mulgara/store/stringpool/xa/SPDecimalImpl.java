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
import java.math.BigDecimal;
import java.net.URI;
import java.nio.ByteBuffer;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.store.stringpool.*;
import org.mulgara.util.Constants;


/**
 * An SPTypedLiteral that represents xsd:decimal literals.
 *
 * @created 2004-10-05
 * @author David Makepeace
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 * @copyright &copy; 2004 <A href="http://www.PIsoftware.com/">Plugged In Software Pty Ltd</A>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class SPDecimalImpl extends AbstractSPTypedLiteral {

  @SuppressWarnings("unused")
  private final static Logger logger = Logger.getLogger(SPDecimalImpl.class);

  /** An ID used for all Decimal types */
  static final int TYPE_ID = 2; // Unique ID

  /** The offset into the byte buffer to find out the encoding used for this data. */
  static final int END_IDX = Constants.SIZEOF_LONG;

  /**
   * Common abstract constructor for all decimal types.
   * @param subtypeId The subtype, this is the ID for either the full or abbreviated URI.
   * @param typeURI The full URI for the data type.
   */
  SPDecimalImpl(int subtypeId, URI typeURI) {
    super(TYPE_ID, subtypeId, typeURI);
  }


  /**
   * Reads a byte buffer and converts to a BigDecimal, regardless of formatting.
   * @param bb The byte buffer to decode.
   * @return A BigDecimal representing the stored number.
   */
  static BigDecimal decode(ByteBuffer bb) {
    ByteBuffer number = bb;
    if (bb.limit() == END_IDX + 1) {
      byte type = bb.get(END_IDX);
      if (type == SPDecimalExtImpl.END_BYTE) {
        return BigDecimal.valueOf(bb.getLong());
      }
      bb.limit(END_IDX);
      number = bb.slice();
    }
    return new BigDecimal(CHARSET.decode(number).toString());
  }

}


/**
 * This class represents xsd:decimal.
 * The data format is as a string. If the string is exactly the length of a Long
 * (the other format available) then it will be incremented by 1, and a -1 byte appended.
 */
class SPDecimalBaseImpl extends SPDecimalImpl {

  /**
   * The terminating byte, indicating the type of this buffer. Only needed if the buffer
   * is the same length as a long.
   */
  static final byte END_BYTE = -1;

  /** The value of the data. */
  final BigDecimal val;

  /** The string representation of the data. */
  final String lexical;

  /**
   * Creates an xsd:decimal out of a long.
   * @param subtypeId The ID for either the full or abbreviated xsd:decimal.
   * @param typeURI The full or abbreviated URI for xsd:decimal.
   * @param l The long value to store.
   */
  SPDecimalBaseImpl(int subtypeId, URI typeURI, long l) {
    super(subtypeId, typeURI);
    lexical = Long.toString(l);
    val = new BigDecimal(l);
  }


  /**
   * Creates an xsd:decimal out of a BigDecimal.
   * @param subtypeId The ID for either the full or abbreviated xsd:decimal.
   * @param typeURI The full or abbreviated URI for xsd:decimal.
   * @param bd The BigDecimal value to store.
   */
  SPDecimalBaseImpl(int subtypeId, URI typeURI, BigDecimal bd) {
    super(subtypeId, typeURI);
    lexical = bd.toPlainString();
    val = bd;
  }


  /**
   * Creates an xsd:decimal by decoding from a data buffer.
   * @param subtypeId The ID for either the full or abbreviated xsd:decimal.
   * @param typeURI The full or abbreviated URI for xsd:decimal.
   * @param data The data containing the xsd:decimal value.
   */
  SPDecimalBaseImpl(int subtypeId, URI typeURI, ByteBuffer data) {
    super(subtypeId, typeURI);
    ByteBuffer number = data;
    if (data.limit() == END_IDX + 1) {
      assert data.get(END_IDX) == END_BYTE;
      data.limit(END_IDX);
      number = data.slice();
    }
    lexical = CHARSET.decode(number).toString();
    val = new BigDecimal(lexical);
  }


  /**
   * Creates an xsd:decimal by decoding from a string.
   * @param subtypeId The ID for either the full or abbreviated xsd:decimal.
   * @param typeURI The full or abbreviated URI for xsd:decimal.
   * @param lexicalForm The string containing the xsd:decimal value.
   */
  SPDecimalBaseImpl(int subtypeId, URI typeURI, String lexicalForm) {
    super(subtypeId, typeURI);
    this.lexical = lexicalForm;
    val = new BigDecimal(lexicalForm);
  }


  /** @see org.mulgara.store.stringpool.SPObject#getData() */
  public ByteBuffer getData() {
    ByteBuffer data = CHARSET.encode(lexical);
    if (data.limit() == END_IDX) {
      ByteBuffer newData = ByteBuffer.allocate(END_IDX + 1);
      newData.put(data);
      newData.put(END_IDX, END_BYTE);
      data = newData;
    }
    return data;
  }


  /** @see org.mulgara.store.stringpool.SPObject#getSPComparator() */
  public SPComparator getSPComparator() {
    return SPDecimalBaseComparator.getInstance();
  }


  /** @see org.mulgara.store.stringpool.SPObject#getLexicalForm() */
  public String getLexicalForm() {
    return lexical;
  }


  /** @see org.mulgara.store.stringpool.AbstractSPTypedLiteral#compareTo(org.mulgara.store.stringpool.SPObject) */
  public int compareTo(SPObject o) {
    // Compare types.
    int c = super.compareTo(o);
    if (c != 0) return c;

    // Compare the longs.
    if (o instanceof SPDecimalExtImpl) {
      long ol = ((SPDecimalExtImpl)o).l;
      return val.compareTo(BigDecimal.valueOf(ol));
    }
    SPDecimalBaseImpl di = (SPDecimalBaseImpl)o;
    return val.compareTo(di.val);
  }


  /** @see java.lang.Object#hashCode() */
  public int hashCode() {
    return lexical.hashCode();
  }


  /** @see java.lang.Object#equals(java.lang.Object) */
  public boolean equals(Object obj) {
    // Check for null.
    if (obj == null) return false;

    try {
      SPDecimalBaseImpl di = (SPDecimalBaseImpl)obj;
      return lexical.equals(di.lexical);
    } catch (ClassCastException ex) {
      // obj was not an SPDecimalImpl.
      return false;
    }
  }

  /** Compares the binary representations of two SPDecimalBaseImpl objects. */
  public static class SPDecimalBaseComparator implements SPComparator {

    /** The singleton instance of this class. */
    private static final SPDecimalBaseComparator INSTANCE = new SPDecimalBaseComparator();

    /**
     * @return The singleton instance of this class.
     */
    public static SPDecimalBaseComparator getInstance() {
      return INSTANCE;
    }

    /**
     * @see org.mulgara.store.stringpool.SPComparator#comparePrefix(java.nio.ByteBuffer, java.nio.ByteBuffer, int)
     * @return Always 0, since this cannot compare on prefixes alone.
     */
    public int comparePrefix(ByteBuffer d1, ByteBuffer d2, int d2Size) {
      return 0;
    }

    /**
     * @see org.mulgara.store.stringpool.SPComparator#compare(java.nio.ByteBuffer, java.nio.ByteBuffer)
     * This comparator WILL compare between xsd:decimal and the extending types
     */
    public int compare(ByteBuffer d1, ByteBuffer d2) {
      return decode(d1).compareTo(decode(d2));
    }

  }

}


/**
 * This class represents extensions of xsd:decimal.
 * The data format is as a long, followed by a byte marker set to 0. This is to
 * distinguish the data format from xsd:decimal, which is stored as a string.
 */
class SPDecimalExtImpl extends SPDecimalImpl {

  /** The terminating byte, to distinguish the data types from SPDecimalBaseImpl */
  static final byte END_BYTE = 0;

  /** The long value containing the number. */
  final Long l;

  /**
   * Creates an xsd:decimal extension out of a long.
   * @param subtypeId The ID for either the full or abbreviated URI.
   * @param typeURI The full or abbreviated URI.
   * @param l The long value to store.
   */
  SPDecimalExtImpl(int subtypeId, URI typeURI, long l) {
    super(subtypeId, typeURI);
    this.l = l;
  }


  /**
   * Creates an xsd:decimal extension out of a buffer.
   * @param subtypeId The ID for either the full or abbreviated URI.
   * @param typeURI The full or abbreviated URI.
   * @param data The buffer containing the data encoding the value.
   */
  SPDecimalExtImpl(int subtypeId, URI typeURI, ByteBuffer data) {
    super(subtypeId, typeURI);
    assert data.limit() == Constants.SIZEOF_LONG + 1;
    int end = data.limit() - 1;
    assert data.get(end) == END_BYTE;
    l = data.getLong();
  }


  /**
   * Creates an xsd:decimal extension out of a string.
   * @param subtypeId The ID for either the full or abbreviated URI.
   * @param typeURI The full or abbreviated URI.
   * @param lexicalForm The string containing the value.
   */
  SPDecimalExtImpl(int subtypeId, URI typeURI, String lexicalForm) {
    super(subtypeId, typeURI);
    l = Long.valueOf(lexicalForm);
  }


  /** @see org.mulgara.store.stringpool.SPObject#getData() */
  public ByteBuffer getData() {
    ByteBuffer data = ByteBuffer.allocate(Constants.SIZEOF_LONG + 1);
    data.putLong(l);
    data.put((byte)END_BYTE);
    data.flip();
    return data;
  }


  /** @see org.mulgara.store.stringpool.SPObject#getSPComparator() */
  public SPComparator getSPComparator() {
    return SPDecimalExtComparator.getInstance();
  }


  /** @see org.mulgara.store.stringpool.SPObject#getLexicalForm() */
  public String getLexicalForm() {
    return Long.toString(l);
  }


  /** @see org.mulgara.store.stringpool.AbstractSPTypedLiteral#compareTo(org.mulgara.store.stringpool.SPObject) */
  public int compareTo(SPObject o) {
    // Compare types.
    int c = super.compareTo(o);
    if (c != 0) return c;

    // The super will have returned a value already, but just in case we need to compare
    // values between different types, we convert.
    if (o instanceof SPDecimalBaseImpl) {
      return -((SPDecimalBaseImpl)o).compareTo(this);
    }
    // Compare the longs.
    SPDecimalExtImpl di = (SPDecimalExtImpl)o;
    return compare(l, di.l);
  }


  /** @see java.lang.Object#hashCode() */
  public int hashCode() {
    return (int)(l * 7) | (int)(l >> 32);
  }


  /** @see java.lang.Object#equals(java.lang.Object) */
  public boolean equals(Object obj) {
    // Check for null.
    if (obj == null) return false;

    try {
      SPDecimalExtImpl di = (SPDecimalExtImpl)obj;
      return l == di.l;
    } catch (ClassCastException ex) {
      // obj was not an SPDecimalExtImpl.
      return false;
    }
  }


  /**
   * Utility for comparing long values.
   * @param l1 The first long.
   * @param l2 The second long.
   * @return +1 if l1 > l2, -1 if l1 < l2, and 0 if l1 == l2
   */
  public static int compare(long l1, long l2) {
    return l1 < l2 ? -1 : (l1 > l2 ? 1 : 0);
  }


  /** Compares the binary representations of two SPDecimalExtImpl objects. */
  public static class SPDecimalExtComparator implements SPComparator {

    /** The singleton instance of this object. */
    private static final SPDecimalExtComparator INSTANCE = new SPDecimalExtComparator();

    /** @return The singleton instance of this object. */
    public static SPDecimalExtComparator getInstance() {
      return INSTANCE;
    }

    /**
     * @see org.mulgara.store.stringpool.SPComparator#comparePrefix(java.nio.ByteBuffer, java.nio.ByteBuffer, int)
     * @return Always 0, since all data is needed for comparing on this class.
     */
    public int comparePrefix(ByteBuffer d1, ByteBuffer d2, int d2Size) {
      return 0;
    }

    /**
     * @see org.mulgara.store.stringpool.SPComparator#compare(java.nio.ByteBuffer, java.nio.ByteBuffer)
     * This comparator WILL compare between xsd:decimal and the extending types
     */
    public int compare(ByteBuffer d1, ByteBuffer d2) {
      if (d1.get(END_IDX) == END_BYTE && d2.get(END_IDX) == END_BYTE) {
        return SPDecimalExtImpl.compare(d1.getLong(), d2.getLong());
      }
      return SPDecimalExtImpl.compare(d1.getLong(), d2.getLong());
    }

  }

}