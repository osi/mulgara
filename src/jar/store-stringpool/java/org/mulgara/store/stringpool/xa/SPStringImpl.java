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
import java.nio.ByteBuffer;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.store.stringpool.*;


/**
 * An SPObject that represents untyped string literals.
 *
 * @created 2002-03-07
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
public final class SPStringImpl extends AbstractSPObject implements SPString {

  private final static Logger logger = Logger.getLogger(SPStringImpl.class);

  private String str;


  SPStringImpl(String str) {
    if (str == null) {
      throw new IllegalArgumentException("Null \"str\" parameter");
    }

    this.str = str;
  }


  SPStringImpl(ByteBuffer data) {
    this.str = CHARSET.decode(data).toString();
  }


  static SPObject newSPObject(String str) {
    return new SPStringImpl(str);
  }


  public String getLexicalForm() {
    return str;
  }


  /* from SPObject interface. */

  public TypeCategory getTypeCategory() {
    return TypeCategory.UNTYPED_LITERAL;
  }


  public ByteBuffer getData() {
    return CHARSET.encode(str);
  }


  public SPComparator getSPComparator() {
    return SPCaseInsensitiveStringComparator.getInstance();
  }


  public String getEncodedString() {
    StringBuffer sb = new StringBuffer(str.length() + 8);
    sb.append(str);
    escapeString(sb);
    return sb.insert(0, '"').append('"').toString();
  }


  public org.jrdf.graph.Node getRDFNode() {
    return new LiteralImpl(str);
  }


  /* from Comparable interface. */

  public int compareTo(Object o) {
    // Compare types.
    int c = super.compareTo(o);
    if (c != 0) return c;

    // Compare the Strings.
    return str.compareToIgnoreCase(((SPStringImpl)o).str);
  }


  /* from Object. */

  public int hashCode() {
    return str.hashCode();
  }


  public boolean equals(Object obj) {
    // Check for null.
    if (obj == null) return false;

    try {
      return str.equals(((SPStringImpl)obj).str);
    } catch (ClassCastException ex) {
      // obj was not an SPStringImpl.
      return false;
    }
  }

}

