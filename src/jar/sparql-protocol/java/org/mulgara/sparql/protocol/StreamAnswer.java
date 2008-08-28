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

package org.mulgara.sparql.protocol;

// Java 2 standard packages
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

// Third party packages
import javax.xml.namespace.QName;
import javax.xml.stream.*;       // JSR 173: Streaming API for XML
import org.apache.log4j.Logger;  // Apache Log4J
import org.jrdf.graph.Literal;   // JRDF
import org.jrdf.graph.URIReference;
import org.jrdf.vocabulary.RDF;

// Local packages
import org.mulgara.query.Answer;
import org.mulgara.query.Cursor;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.URIReferenceImpl;

/**
* An answer backed by an XML-formatted stream.
*
* @created 2004-03-21
* @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
* @version $Revision: 1.9 $
* @modified $Date: 2005/01/05 04:59:05 $ by $Author: newmana $
* @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
* @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
*      Software Pty Ltd</a>
* @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
*/
public class StreamAnswer implements Answer, XMLStreamConstants
{
  /**
  * Logger.
  */
  private static final Logger logger =
    Logger.getLogger(StreamAnswer.class.getName());

  /**
  * The RDF namespace used in the serialization
  */
  public static final String NAMESPACE = "java:"+StreamAnswer.class+"#";

  /**
  * The current row.
  *
  * The first row is 0.
  */
  private long row = -1;

  /**
  * The row count obtained from the <code>row-count</code> processing
  * instruction.
  *
  * If the processing instruction hasn't been encountered, this field will
  * have the magical {@link #ROW_COUNT_UNAVAILABLE} value;
  */
  private long rowCount = ROW_COUNT_UNAVAILABLE;

  /**
  * A magical value for {@link #rowCount} indicating that there is no known
  * row count.
  */
  private static long ROW_COUNT_UNAVAILABLE = -1;

  /**
  * The variables occuring in this answer.
  */
  private Variable[] variables = null;

  /**
  * The XML stream.
  */
  private final XMLStreamReader xmlStreamReader;

  //
  // Constructor
  //

  /**
  * Construct an answer based upon an XML-formatted input stream.
  *
  * @param inputStream  an XML-formatted input stream
  * @throws IllegalArgumentException if <var>inputStream</var> is
  *   <code>null</code>
  * @throws StreamFormatException if the <var>inputStream</var> is misformatted
  */
  public StreamAnswer(InputStream inputStream) throws StreamFormatException
  {
    // Validate "inputStream" parameter
    if (inputStream == null) {
      throw new IllegalArgumentException("Null \"inputStream\" parameter");
    }

    // Block until we've read enough metadata from the header to initialize
    // the rest of the fields
    try {
      xmlStreamReader =
        XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
      while (xmlStreamReader.hasNext()) {
        logger.debug("stream has next");
        switch(xmlStreamReader.next()) {
        case PROCESSING_INSTRUCTION:
          logger.debug("Processing instruction: "+xmlStreamReader.getPITarget()+
                       "=\""+xmlStreamReader.getPIData()+"\"");
          if ("itql-row-count".equals(xmlStreamReader.getPITarget())) {
            try {
              rowCount = Long.parseLong(xmlStreamReader.getPIData().trim());
            }
            catch (NumberFormatException e) {
              throw new StreamFormatException(
                "Bad itql-row-count: "+xmlStreamReader.getPIData(), e
              );
            }
            assert rowCount != ROW_COUNT_UNAVAILABLE;
          }
          else if ("itql-variables".equals(xmlStreamReader.getPITarget())) {
            StringTokenizer stringTokenizer =
              new StringTokenizer(xmlStreamReader.getPIData());
            List list = new ArrayList();
            while (stringTokenizer.hasMoreTokens()) {
              list.add(new Variable(stringTokenizer.nextToken()));
            }
            variables = (Variable[]) list.toArray(new Variable[list.size()]);
            assert variables != null;
          }
          break;

        case START_ELEMENT:
          if (logger.isDebugEnabled()) {
            logger.debug("document element "+xmlStreamReader.getName());
          }

          if (xmlStreamReader.getName().equals("{"+RDF.BASE_URI+"}RDF")) {
            throw new StreamFormatException("Document element is not RDF");
          }

          // Verify that we know the row count
          if (rowCount == ROW_COUNT_UNAVAILABLE) {
            throw new StreamFormatException(
              "Missing itql-row-count processing instruction in XML stream"
            );
          }

          // Verify that we know the column variables
          if (variables == null) {
            variables = new Variable[] {};
            /*
            throw new StreamFormatException(
              "Missing itql-variables processing instruction in XML stream"
            );
            */
          }

          // Construction is complete
          return;

        default:
          // ignore and keep looping
          logger.debug("skip");
        }
      }
      logger.debug("stream has next");

      throw new StreamFormatException("Unexpected end of stream");
    }
    catch (XMLStreamException e) {
      throw new StreamFormatException("Bad XML in answer stream", e);
    }
  }

  //
  // Methods implementing Answer
  //

  /**
  * METHOD TO DO
  *
  * @return RETURNED VALUE TO DO
  */
  public Object clone()
  {
    throw new RuntimeException("Unable to clone "+getClass());
  }

  /**
  * Return the object at the given index.
  *
  * @param column  column numbering starts from zero
  * @return the value at the given index
  * @throws TuplesException EXCEPTION TO DO
  */
  public Object getObject(int column) throws TuplesException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("getObject("+column+") = "+variables[column]);
    }

    try {
      while (xmlStreamReader.hasNext()) {
        switch (xmlStreamReader.next()) {
        case START_ELEMENT:
          if (logger.isDebugEnabled()) {
            logger.debug("start element (variable) "+xmlStreamReader.getName());
          }

          if ("Solution".equals(xmlStreamReader.getName())) {
            throw new StreamFormatException(
              "Unexpected end of row at column "+column
            );
          }

          QName varQName = new QName(variables[column].getName());
          if (!varQName.equals(xmlStreamReader.getName())) {
            throw new StreamFormatException(
              "Column "+column+" should be "+variables[column].getName()+
              ", not "+xmlStreamReader.getName()
            );
          }

          if (xmlStreamReader.getAttributeCount() > 0) {
            assert "resource".equals(xmlStreamReader.getAttributeName(0));
            try {
              URIReference uriReference = new URIReferenceImpl(new URI(
                xmlStreamReader.getAttributeValue(0)
              ));
              String skipped = xmlStreamReader.getElementText();
              logger.debug("Got "+variables[column]+"="+uriReference+
                           ", skipped \""+skipped+"\"");
              return uriReference;
            }
            catch (URISyntaxException e) {
              throw new TuplesException(
                "Misformatted rdf:resource attribute", e
              );
            }
          }
          else {
            Literal literal = new LiteralImpl(xmlStreamReader.getElementText());
            logger.debug("Got "+variables[column]+"="+literal);
            return literal;
          }

        default:
          // everything else is skipped over
        }
      }

      throw new StreamFormatException("Unexpected end of stream");
    }
    catch (StreamFormatException e) {
      logger.warn("Couldn't getObject("+column+")", e);
      throw e;
    }
    catch (XMLStreamException e) {
      throw new TuplesException("Couldn't read XML stream", e);
    }
  }

  /**
  * Return the object at the given column name.
  *
  * @param columnName the index of the object to retrieve
  * @return the value at the given index
  * @throws SQLException on failure
  * @throws TuplesException EXCEPTION TO DO
  */
  public Object getObject(String columnName) throws TuplesException
  {
    return new LiteralImpl("dummy");
  }

  /**
  * Reset to iterate through every single element.
  *
  * @throws TuplesException EXCEPTION TO DO
  */
  public void beforeFirst() throws TuplesException
  {
    row = -1;
    //throw new TuplesException("Unable to reset stream");
  }

  /**
  * Free resources associated with this instance.
  *
  * @throws TuplesException EXCEPTION TO DO
  */
  public void close() throws TuplesException
  {
    try {
      xmlStreamReader.close();
    }
    catch (XMLStreamException e) {
      throw new TuplesException("Unable to close stream", e);
    }
  }

  /**
  * Find the index of a variable.
  *
  * @param column PARAMETER TO DO
  * @return The ColumnIndex value
  * @throws TuplesException EXCEPTION TO DO
  */
  public int getColumnIndex(Variable column) throws TuplesException
  {
    assert variables != null;

    // See if any of the columns match the requested variable
    for (int i=0; i<variables.length; i++) {
      if (variables[i].equals(column)) {
        return i;
      }
    }

    // None of the columns matched the requested variable
    throw new TuplesException("No such column "+column);
  }

  /**
  * Returns the number of variables (columns).
  *
  * @return the number of variables (columns)
  */
  public int getNumberOfVariables()
  {
    return variables.length;
  }

  /**
  * The variables bound and their default collation order. The array returned
  * by this method should be treated as if its contents were immutable, even
  * though Java won't enforce this. If the elements of the array are modified,
  * there may be side effects on the past and future clones of the tuples it
  * was obtained from.
  *
  * @return the {@link Variable}s bound within this answer.
  */
  public Variable[] getVariables()
  {
    return variables;
  }

  /**
  * Tests whether this is a unit-valued answer. A unit answer appended to
  * something yields the unit answer. A unit answer joined to something yields
  * the same something. Notionally, the unit answer has zero columns and one
  * row.
  *
  * @return The Unconstrained value
  * @throws TuplesException EXCEPTION TO DO
  */
  public boolean isUnconstrained() throws TuplesException
  {
    throw new TuplesException("Is Unconstrained not implemented");
  }

  /**
  * This method returns the number of rows which this instance contains.
  *
  * @return an upper bound on the number of rows that this instance contains.
  * @throws TuplesException EXCEPTION TO DO
  */
  public long getRowCount() throws TuplesException
  {
    // Make sure we have a row count to return
    if (rowCount == ROW_COUNT_UNAVAILABLE) {
      throw new TuplesException("Row count not available via BEEP");
    }

    return rowCount;
  }

  public long getRowUpperBound() throws TuplesException
  {
    return getRowCount();
  }

  public int getRowCardinality() throws TuplesException
  {
    if (getRowCount() > 1) {
      return Cursor.MANY;
    }
    switch ((int)getRowCount()) {
      case 0:
        return Cursor.ZERO;
      case 1:
        return Cursor.ONE;
      default:
        throw new TuplesException("Illegal Row Count: " + getRowCount());
    }
  }

  /**
  * Move to the next row.
  *
  * If no such row exists, return <code>false<code> and the current row
  * becomes unspecified.  The current row is unspecified when an
  * instance is created.  To specify the current row, the
  * {@link #beforeFirst()} method must be invoked
  *
  * @return whether a subsequent row exists
  * @throws IllegalStateException if the current row is unspecified.
  * @throws TuplesException EXCEPTION TO DO
  */
  public boolean next() throws TuplesException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("next()");
    }

    try {
      QName solutionQName = new QName("Solution");

      /*
      // Obtain </Solution>
      A:while (xmlStreamReader.hasNext()) {
        logger.debug("stream has next");
        switch (xmlStreamReader.next()) {
        case END_ELEMENT:
          logger.debug("End element "+xmlStreamReader.getName());
          if (solutionQName.equals(xmlStreamReader.getName())) {
            break A;
          }

        default:
          // skip past anything else
          logger.debug("Skip");
        }
      }
      */

      // Obtain <Solution>
      while (xmlStreamReader.hasNext()) {
        logger.debug("stream has next");
        switch (xmlStreamReader.next()) {
        case END_DOCUMENT:
          logger.debug("next: false");
          return false;

        case START_ELEMENT:
          QName foo = xmlStreamReader.getName();
          if (solutionQName.equals(foo)) {
            logger.debug("next: true");
            return true;
          }
          else {
            logger.debug("unexpected start element (Solution) \""+foo+"\"");
          }

        default:
          // skip past anything else
          logger.debug("Skip");
        }
      }
      logger.debug("stream has no next");

      throw new StreamFormatException("Unexpected end of stream");
    }
    catch (XMLStreamException e) {
      throw new TuplesException("Couldn't read XML stream", e);
    }
  }

  //
  // Static library methods
  //

  /**
  * Serialize an {@link Answer} to an {@link OutputStream} in a format that
  * can be parsed by a {@link StreamAnswer}.
  *
  * @param answer  the instance to serialize
  * @param outputStream  the stream to serial to
  * @throws IllegalArgumentException if <var>answer</var> or
  *   <var>outputStream</var> are <code>null</code>
  * @throws TuplesException if the <var>answer</var> can't be read from
  * @throws XMLStreamException if the <var>outputStream</var> can't be written
  */
  public static void serialize(Answer answer, OutputStream outputStream)
    throws TuplesException, XMLStreamException
  {
    // Validate "answer" parameter
    if (answer == null) {
      throw new IllegalArgumentException("Null \"answer\" parameter");
    }

    // Validate "outputStream" parameter
    if (outputStream == null) {
      throw new IllegalArgumentException("Null \"outputStream\" parameter");
    }

    // Obtain a writer
    XMLStreamWriter xmlStreamWriter =
      XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream);

    // Write the Answer's metadata
    serializeMetadata(answer, xmlStreamWriter);

    // Write the Answer's solutions
    xmlStreamWriter.writeStartElement("RDF");
    xmlStreamWriter.setPrefix("rdf", RDF.BASE_URI.toString());
    xmlStreamWriter.setDefaultNamespace(NAMESPACE);
    xmlStreamWriter.writeNamespace("rdf", RDF.BASE_URI.toString());
    serializeSolutions(answer, xmlStreamWriter);
    xmlStreamWriter.writeEndElement();

    xmlStreamWriter.close();
  }

  /**
  * Serialize metadata.
  *
  * @param answer  the instance to serialize
  * @param xmlStreamWriter  the XML stream to serialize to
  * @throws XMLStreamException if the <var>xmlStreamWriter</var> can't be
  *   written to
  */
  private static void serializeMetadata(Answer          answer,
                                        XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException
  {
    // Write the <?itql-row-count?> processing instruction
    try {
      xmlStreamWriter.writeProcessingInstruction(
        "itql-row-count", " "+Long.toString(answer.getRowCount())
      );
    }
    catch (TuplesException e) {
      // couldn't read row count from the source, so won't be able to read
      // row count from the serialized stream either
    }

    // Write the <?itql-variables?> processing instruction
    Variable[] variables = answer.getVariables();
    if (variables.length > 0) {
      StringBuffer stringBuffer = new StringBuffer();
      for (int i=0; i<variables.length; i++) {
        stringBuffer.append(" ").append(variables[i].getName());
      }
      xmlStreamWriter.writeProcessingInstruction(
        "itql-variables", stringBuffer.toString()
      );
    }
  }

  /**
  * Serialize solutions.
  *
  * @param answer  the instance to serialize
  * @param xmlStreamWriter  the XML stream to serialize to
  * @throws TuplesException if the <var>answer</var> can't be read from
  * @throws XMLStreamException if the <var>xmlStreamWriter</var> can't be
  *   written to
  */
  private static void serializeSolutions(Answer          answer,
                                         XMLStreamWriter xmlStreamWriter)
    throws TuplesException, XMLStreamException
  {
    Variable[] variables = answer.getVariables();

    while (answer.next()) {
      xmlStreamWriter.writeStartElement("Solution");
      for (int i=0; i<variables.length; i++) {
        Object object = answer.getObject(i);
        if (object != null) {
          xmlStreamWriter.writeStartElement(variables[i].getName());

          if (object instanceof Answer) {
            xmlStreamWriter.writeAttribute(
              RDF.BASE_URI.toString(), "parseType", "Collection"
            );
            serializeMetadata((Answer) object, xmlStreamWriter);
            serializeSolutions((Answer) object, xmlStreamWriter);
          }
          else if (object instanceof Literal) {
            Literal literal = (Literal) object;
            if (literal.getDatatypeURI() != null) {
              xmlStreamWriter.writeAttribute(
                RDF.BASE_URI.toString(),
                "datatype",
                literal.getDatatypeURI().toString()
              );
            }
            xmlStreamWriter.writeCharacters(literal.getLexicalForm());
          }
          else if (object instanceof URIReference) {
            xmlStreamWriter.writeAttribute(
              RDF.BASE_URI.toString(), "resource", object.toString()
            );
          }
          else {
            throw new Error(
              "Answer contains "+object+" of class "+object.getClass()
            );
          }
          
          xmlStreamWriter.writeEndElement();
        }
      }
      xmlStreamWriter.writeEndElement();
    }
  }
}
