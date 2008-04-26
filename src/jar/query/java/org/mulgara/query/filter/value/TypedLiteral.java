/**
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
package org.mulgara.query.filter.value;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mulgara.query.QueryException;
import org.mulgara.query.filter.Context;
import org.mulgara.query.filter.ContextOwner;


/**
 * A literal with a URI type and a value.
 *
 * @created Mar 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class TypedLiteral extends AbstractComparableLiteral {

  /** Generated Serialization ID for RMI */
  private static final long serialVersionUID = -4765911650373775807L;

  /** The type URI for this literal */
  private URI type;
  
  /** The lexical representation for this literal */
  private String lexical;

  /** The namespace for all XSD types */
  public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";

  /**
   * Creates the value to wrap the string
   * @param value The data to wrap
   * @param type The type of the literal
   */
  public TypedLiteral(Object value, URI type) {
    super(value);
    lexical = value.toString();  // lexical == value if value instanceof String
    this.type = type;
  }

  /**
   * A factory for typed literals.
   * @param value The literal data in its lexical form. This means it's a String, and can even be invalid
   * @param type The type of the literal. May be null.
   * @param lang The language code of the literal. May be null.
   */
  public static ValueLiteral newLiteral(String value, URI type, String lang) {
    if (type != null) {
      // get the info registered for this type URI
      TypeInfo info = infoMap.get(type);
      if (info != null && info.delegatedConstruction()) {
        try {
          return newLiteral(info.toData(value));
        } catch (QueryException e) {  // should not happen
          throw new AssertionError("Internal type maps are inconsistent for: " + info.getClass().getSimpleName());
        }
      }
      // no type info for the given URI, just pass through as a general typed literal
      return new TypedLiteral(value, type);
    }
    // no type info provided, so it's a simple string
    if (lang == null || lang.length() == 0) return new SimpleLiteral(value);
    return new SimpleLiteral(value, lang);
  }

  /**
   * A factory for typed literals from raw Java types. This is most likely to come
   * from literal numbers parsed by a SPARQL parser.
   * @param value The data as an object. May be a String, or some kind of {@link java.lang.Number}
   */
  public static ValueLiteral newLiteral(Object value) throws QueryException {
    DataCompare dc = typeMap.get(value.getClass());
    if (dc == null) throw new QueryException("Unrecognized data type: " + value.getClass().getSimpleName());
    return dc.newLiteral(value);
  }

  /**
   * Gets the type of this literal
   * @return The URI for this literals type
   */
  public IRI getType() {
    return new IRI(type);
  }

  /** @see org.mulgara.query.filter.value.ValueLiteral#isSimple() */
  public boolean isSimple() throws QueryException {
    return false;
  }

  /**
   * Gets the language ID of this literal
   * @return The language ID for this literal
   */
  public SimpleLiteral getLang() {
    return SimpleLiteral.EMPTY;
  }
  
  /** {@inheritDoc} */
  public String getLexical() {
    return lexical;
  }

  /**
   * No context needed as this is a literal value.
   * @see org.mulgara.query.filter.RDFTerm#getContextOwner()
   */
  public ContextOwner getContextOwner() {
    return null;
  }

  /**
   * A public string representation of this literal.
   */
  public String toString() {
    return "'" + lexical + "'^^<" + type + ">";
  }

  /**
   * No context needed as this is a literal value.
   * @see org.mulgara.query.filter.RDFTerm#setContextOwner(org.mulgara.query.filter.ContextOwner)
   */
  public void setContextOwner(ContextOwner owner) { }

  /** {@inheritDoc} */
  public boolean test(Context context) throws QueryException {
    if (type == null) return ((String)value).length() != 0;
    TypeInfo test = infoMap.get(type);
    if (test == null) throw new QueryException("Type Error: no effective boolean value for: " + toString());
    return test.ebv(value.toString());
  }

  /** A map of XSD datatypes onto the tests for their types */
  private static Map<URI,TypeInfo> infoMap = new HashMap<URI,TypeInfo>();

  private static final URI xsd(String s) { return URI.create(XSD_NS + s); }

  /** This interface tests if datatype matches the data to give an EBV of <code>true</code> */
  public interface TypeInfo {
    /** Returns an EBV of <code>true</code> iff the data matches the type sufficiently */
    public boolean ebv(String data) throws QueryException;
    /** Returns data parsed out of the string literal */
    public Object toData(String representation);
    /** Returns the URI for this type */
    public URI getTypeURI();
    /** Indicates if construction should be delegated */
    public boolean delegatedConstruction();
    /** All the registered types */
    static final List<TypeInfo> types = new ArrayList<TypeInfo>();
  }

  /** Simple extension to TypeInfo to store the type URI for all implementing classes  */
  private static abstract class AbstractXSD implements TypeInfo  {
    private final URI typeURI;
    AbstractXSD(String fragment) { typeURI = xsd(fragment); }
    public URI getTypeURI() { return typeURI; }
    public boolean delegatedConstruction() { return true; }
  }

  /**
   * Helper method for static initialization
   * @param info The info to add to the infoMap
   */
  private static void addDefaultTypeInfo(TypeInfo info) {
    infoMap.put(info.getTypeURI(), info);
  }

  // initialize the types
  static {
    addDefaultTypeInfo(new XSDString());
    addDefaultTypeInfo(new XSDBoolean());
    addDefaultTypeInfo(new XSDDouble());
    addDefaultTypeInfo(new XSDFloat());
    addDefaultTypeInfo(new XSDLong());
    addDefaultTypeInfo(new XSDInteger());
    addDefaultTypeInfo(new XSDShort());
    addDefaultTypeInfo(new XSDByte());
    addDefaultTypeInfo(new XSDDate());
    infoMap.put(xsd("decimal"), new XSDLong());
    infoMap.put(xsd("integer"), new XSDLong());
    infoMap.put(xsd("nonPositiveInteger"), new XSDLong());
    infoMap.put(xsd("negativeInteger"), new XSDLong());
    infoMap.put(xsd("nonNegativeInteger"), new XSDLong());
    infoMap.put(xsd("positiveInteger"), new XSDInteger());
    infoMap.put(xsd("unsignedLong"), new XSDLong());
    infoMap.put(xsd("unsignedInt"), new XSDLong());
    infoMap.put(xsd("unsignedShort"), new XSDInteger());
    infoMap.put(xsd("unsignedByte"), new XSDShort());
  }

  //////////////////////////////////////////////////////////////////
  // Implementing classes
  //////////////////////////////////////////////////////////////////

  static class XSDString extends AbstractXSD {
    XSDString() { super("string"); }
    public boolean ebv(String data) { return data != null && data.length() != 0; }
    public Object toData(String r) { return r; }
    public boolean delegatedConstruction() { return false; }
  }

  private static class XSDBoolean extends AbstractXSD {
    XSDBoolean() { super("boolean"); }
    public boolean ebv(String data) { return Boolean.parseBoolean(data); }
    public Object toData(String r) { return Boolean.parseBoolean(r); }
  }
  
  private static class XSDDouble extends AbstractXSD {
    XSDDouble() { super("double"); }
    public boolean ebv(String data) {
      try {
        if (data == null) return false;
        Double d = Double.parseDouble(data);
        return 0 != Double.parseDouble(data) && !d.isNaN();
      } catch (NumberFormatException nfe) {
        return false;
      }
    }
    public Object toData(String r) {
      try {
        return Double.parseDouble(r);
      } catch (NumberFormatException nfe) {
        return Double.valueOf(0.0);
      }
    }
  }
  
  private static class XSDFloat extends AbstractXSD {
    XSDFloat() { super("float"); }
    public boolean ebv(String data) {
      try {
        if (data == null) return false;
        Float f = Float.parseFloat(data);
        return 0 != f && !f.isNaN();
      } catch (NumberFormatException nfe) {
        return false;
      }
    }
    public Object toData(String r) {
      try {
        return Float.parseFloat(r);
      } catch (NumberFormatException nfe) {
        return Float.valueOf(0);
      }
    }
  }

  private static class XSDLong extends AbstractXSD {
    XSDLong() { super("long"); }
    public boolean ebv(String data) {
      try {
        return data != null && 0 != Long.parseLong(data);
      } catch (NumberFormatException nfe) {
        return false;
      }
    }
    public Object toData(String r) {
      try {
        return Long.parseLong(r);
      } catch (NumberFormatException nfe) {
        return Long.valueOf(0);
      }
    }
  }

  private static class XSDInteger extends AbstractXSD {
    XSDInteger() { super("int"); }
    public boolean ebv(String data) {
      try {
        return data != null && 0 != Integer.parseInt(data);
      } catch (NumberFormatException nfe) {
        return false;
      }
    }
    public Object toData(String r) {
      try {
        return Integer.parseInt(r);
      } catch (NumberFormatException nfe) {
        return Integer.valueOf(0);
      }
    }
  }

  private static class XSDShort extends AbstractXSD {
    XSDShort() { super("short"); }
    public boolean ebv(String data) {
      try {
        return data != null && 0 != Short.parseShort(data);
      } catch (NumberFormatException nfe) {
        return false;
      }
    }
    public Object toData(String r) {
      try {
        return Short.parseShort(r);
      } catch (NumberFormatException nfe) {
        return Short.valueOf((short)0);
      }
    }
  }

  private static class XSDByte extends AbstractXSD {
    XSDByte() { super("byte"); }
    public boolean ebv(String data) {
      try {
        return data != null && 0 != Byte.parseByte(data);
      } catch (NumberFormatException nfe) {
        return false;
      }
    }
    public Object toData(String r) {
      try {
        return Long.parseLong(r);
      } catch (NumberFormatException nfe) {
        return Byte.valueOf((byte)0);
      }
    }
  }

  private static class XSDDate extends AbstractXSD {
    XSDDate() { super("dateTime"); }
    public boolean ebv(String data) throws QueryException { throw new QueryException("Unable to convert a date to a boolean"); }
    public Object toData(String r) { return DateTime.parseDate(r); }
  }

}
