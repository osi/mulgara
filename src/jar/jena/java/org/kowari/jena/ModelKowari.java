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

package org.kowari.jena;

// Java 2 standard packages
import java.io.*;
import java.net.*;
import java.util.*;

// Log4j
import org.apache.log4j.Category;

// JRDF
import org.jrdf.vocabulary.*;

// SAX
import org.xml.sax.helpers.*;
import org.apache.xerces.parsers.*;
import org.xml.sax.*;

// Jena Packages
import com.hp.hpl.jena.enhanced.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.shared.impl.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

// Local packages
import org.kowari.query.ModelResource;
import org.kowari.query.QueryException;
import org.kowari.query.Value;
import org.kowari.query.rdf.*;
import org.kowari.util.TempDir;
import org.kowari.server.*;

/**
 * An implementation of {@link com.hp.hpl.jena.rdf.model.Model}.
 *
 * @created 2004-02-20
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/07 09:37:07 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ModelKowari extends ModelCom implements Model {

  /**
   * Logger. This is named after the class.
   */
  private final static Category logger =
      Category.getInstance(ModelKowari.class.getName());

  /**
   * Local graph - Kowari.
   */
  private GraphKowari graphKowari;

  /**
   * Create a new Kowari Model based on the given Kowari Graph.
   *
   * @param graph the graph to be used by the model.
   */
  public ModelKowari(GraphKowari graph) {

    super(graph, BuiltinPersonalities.model);
    graphKowari = graph;
  }

  public NsIterator listNameSpaces()  {
    ExtendedIterator predIter = graphKowari.findUniquePredicates();
    ExtendedIterator typeIter = graphKowari.find(Node.ANY,
        graphKowari.getJenaFactory().convertValueToNode((Value)
        new URIReferenceImpl(RDF.TYPE)), Node.ANY);
    predIter.andThen(typeIter);

    return new NsIteratorImpl(predIter, null);
  }

  public Model add(Resource s, Property p, RDFNode o) {
    modelReifier.noteIfReified(s, p, o);
    Triple t = new Triple(s.asNode(), p.asNode(), o.asNode());
    graph.add(t);
    return this;
  }

  public StmtIterator listStatements()  {
      return IteratorFactory.asStmtIterator(GraphUtil.findAll(graph), this);
  }

  public Model read(Reader reader, String base)  {
    return read(reader, base, null);
  }

  public Model read(Reader reader, String base, String lang) {
    File tmpFile = null;

    // Try to parse base - if successful set as property.
    try {
      if (base == null) {
        base = "";
      }

      tmpFile = writeReaderToFile(reader);
      String newLang = convertLang(lang);

      URI baseURI;

      // Ensure if of type n3 ends with .n3 file extension.
      if (newLang.equals("N3")) {
        baseURI = new URI((base + ".n3"));
      }
      // Else we'll assume RDF/XML which is the default.
      else {
        baseURI = new URI(base);
      }

      // Read the namespaces first.
      if (newLang.startsWith("RDF/XML")) {
        new NamespaceReader().read(this, new FileInputStream(tmpFile), base);
      }

      // Load the data.
      graphKowari.getSession().setModel(new FileInputStream(tmpFile),
          graphKowari.graphURI, new ModelResource(baseURI));
    }
    catch (QueryException ge) {
      logger.error("Failed to add statements", ge);
      throw new JenaException("Failed to add statements");
    }
    catch (IOException ioe) {
      logger.error("Failed to parse file", ioe);
      throw new JenaException("Failed to parse file");
    }
    catch (URISyntaxException use) {
      throw new JenaException("Invalid XML base: " + base);
    }
    finally {
      if (tmpFile != null) {
        tmpFile.delete();
      }
    }

    return this;
  }

  public Model read(InputStream reader, String base) {
    return read(reader, base, null);
  }

  public Model read(String url) {
    return read(url, null);
  }

  public Model read(String url, String lang) {

    File tmpFile = null;

    try {

      String newLang = convertLang(lang);
      URLConnection conn = new URL(url).openConnection();
      String encoding = conn.getContentEncoding();
      tmpFile = writeReaderToFile(conn.getInputStream());

      // Load the namespaces in first.
      if (newLang.startsWith("RDF/XML")) {
        new NamespaceReader().read(this, new FileInputStream(tmpFile), url);
      }

      // Ensure if of type n3 ends with .n3 file extension.
      if (newLang.equals("N3")) {
        url = url + ".n3";
      }

      // Load the data.
      graphKowari.getSession().setModel(graphKowari.graphURI,
          new ModelResource(new URI(url)));
    }
    catch (QueryException ge) {
      logger.error("Failed to add statements from: " + url, ge);
      throw new JenaException("Failed to add statements from: " + url);
    }
    catch (IOException ioe) {
      logger.error("Failed to parse file", ioe);
      throw new JenaException("Failed to parse file");
    }
    catch (URISyntaxException use) {
      throw new JenaException("Unable to parse URL: " + url);
    }
    finally {
      if (tmpFile != null) {
        tmpFile.delete();
      }
    }
    return this;
  }

  public Model read(InputStream reader, String base, String lang) {

    File tmpFile = null;

    // Try to parse base - if successful set as property.
    try {
      if (base == null) {
        base = "";
      }

      tmpFile = writeReaderToFile(reader);
      String newLang = convertLang(lang);

      // Read the namespaces first.
      if (newLang.startsWith("RDF/XML")) {
        new NamespaceReader().read(this, new FileInputStream(tmpFile), base);
      }


      URI baseURI;
      // Ensure if of type n3 ends with .n3 file extension.
      if (newLang.equals("N3")) {
        baseURI = new URI(base + ".n3");
      }
      else {
        baseURI = new URI(base);
      }

      // Load the data.
      graphKowari.getSession().setModel(new FileInputStream(tmpFile),
          graphKowari.graphURI, new ModelResource(baseURI));
    }
    catch (QueryException ge) {
      logger.error("Failed to add statements", ge);
      throw new JenaException("Failed to add statements");
    }
    catch (IOException ioe) {
      logger.error("Failed to parse file", ioe);
      throw new JenaException("Failed to parse file");
    }
    catch (URISyntaxException use) {
      throw new JenaException("Invalid XML base: " + base);
    }
    finally {
      if (tmpFile != null) {
        tmpFile.delete();
      }
    }

    return this;
  }

  /**
   * Returns N3 or RDF/XML if the given string starts with N3 or RDF/XML.  If
   * neither will return un-modified.
   *
   * @param lang a string to truncate to N3 or RDF/XML.
   * @return the converted string.
   */
  private String convertLang(String lang) {

    if (lang != null && !lang.equals("")) {
      if (lang.startsWith("N3")) {
        return "N3";
      }
      else if (lang.startsWith("RDF/XML")) {
        return "RDF/XML";
      }
    }
    // Default language
    return "RDF/XML";
  }

  public String toString() {
    return "<ModelKowari  " + getGraph() + " | " + reifiedToString() + ">";
  }

  /**
   * Write out reader to tmp file.
   *
   * @param in InputStream the inputstream to use as the source data.
   * @throws JenaException if there was an IOException.
   * @return the resultant file.
   */
  public File writeReaderToFile(InputStream in) throws JenaException {
    return writeStreamToFile(new BufferedReader(new InputStreamReader(in)));
  }

  /**
   * Write out reader to tmp file.
   *
   * @param in InputStream the inputstream to use as the source data.
   * @throws JenaException if there was an IOException.
   * @return the resultant file.
   */
  public File writeReaderToFile(Reader in) throws JenaException {
    return writeStreamToFile(new BufferedReader(in));
  }

  /**
   * Write out input stream to tmp file.
   *
   * @param in the input stream to save.
   * @return the resultant file.
   */
  private File writeStreamToFile(BufferedReader in) throws JenaException {

    try {
      File tmpFile = TempDir.createTempFile("jena", ".mdl");
      OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(
          new FileOutputStream(tmpFile)));
      char[] buffer = new char[4096];
      int length;
      while ((length = in.read(buffer)) != -1) {
        out.write(buffer, 0, length);
      }

      in.close();
      out.close();

      return tmpFile;
    }
    catch (IOException ioe) {
      throw new JenaException(ioe);
    }
  }

  /**
   * A class which parses a given RDF/XML file and adds the namespaces to a
   * model.
   */
  private class NamespaceReader {

    /**
     * The filter to use to parse the RDF/XML file.
     */
    private NamespaceXMLFilterImpl filter = new NamespaceXMLFilterImpl();

    /**
     * The model to add the prefixes to.
     */
    private Model model;

    /**
     * The error handler to report any problems to.
     */
    private RDFErrorHandler errorHandler = new RDFDefaultErrorHandler();

    /**
     * Reads an input stream adding the new prefix values to the model.
     *
     * @param model the model to add the prefixes to.
     * @param in the source to parse.
     * @param xmlBase the XML base value.
     * @throws JenaException if there is any problem parsing or reading the
     *   the InputSource.
     */
    public void read(final Model model, InputStream in,
        String xmlBase) throws JenaException {
      read(model, new InputSource(in), xmlBase);
    }

    /**
     * Reads an input stream adding the new prefix values to the model.
     *
     * @param model the model to add the prefixes to.
     * @param in the source to parse.
     * @param encoding the file encoding.
     * @param xmlBase the XML base value.
     * @throws JenaException if there is any problem parsing or reading the
     *   the InputSource.
     */
    public void read(final Model model, InputStream in, String encoding,
        String xmlBase) throws JenaException {
      try {
        read(model, new InputSource(new InputStreamReader(in, encoding)), xmlBase);
      }
      catch (IOException e) {
        throw new JenaException(e);
      }
    }

    /**
     * Validate the XML Base and parses the RDF/XML file.
     *
     * @param newModel the model to add the prefixes to.
     * @param inputS InputSource the source to parse.
     * @param xmlBase the XML base value.
     * @throws JenaException if there is any problem parsing or reading the
     *   the InputSource.
     */
    private void read(Model newModel, InputSource inputS,
        String xmlBase) throws JenaException {
      model = newModel;
      if (xmlBase != null && !xmlBase.equals("")) {
        try {
          URI uri = new URI(xmlBase);
        }
        catch (URISyntaxException e) {
          errorHandler.error(e);
        }
      }

      try {
        filter.setModel(model);
        inputS.setSystemId(xmlBase);
        StandardParserConfiguration c = new StandardParserConfiguration();
        filter.setParent(new SAXParser(c));
        filter.parse(inputS);
      }
      catch (IOException e) {
        throw new JenaException(e);
      }
      catch (EndParsingException e) {
        // Do nothing.
      }
      catch (SAXException e) {
        throw new JenaException(e);
      }
    }
  }

  /**
   * A simple filter that parses the names space for the RDF tag.  After this
   * has been parsed we end the parsing by throwing a special kind of
   * SAXException.
   */
  private class NamespaceXMLFilterImpl extends XMLFilterImpl {

    /**
     * The model to add the namespaces to.
     */
    private Model model;

    /**
     * Set the model to add the name spaces encountered in the RDF/XML file.
     *
     * @param newModel the model to call setNsPrefix on.
     */
    public void setModel(Model newModel) {
      model = newModel;
    }

    /**
     * Calls super and if the given URI isNiceURI then adds it to the model.
     */
    public void startPrefixMapping(String prefix,
        String uri) throws SAXException {
      super.startPrefixMapping(prefix, uri);
      if (PrefixMappingImpl.isNiceURI(uri)) {
        model.setNsPrefix(prefix, uri);
      }
    }

    /**
     * Throws an exception after we've gone past the RDF tag.
     *
     * @throws SAXException if there's a normal parsing error.
     * @throws EndParsingException if we've gone past the RDF tag.
     */
    public void startElement(String uri, String localName, String qName,
        Attributes atts) throws SAXException {

      if (!(uri.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#") &&
          (localName.equals("RDF")))) {
        throw new EndParsingException("End parsing");
      }
    }
  }

  /**
   * A simple extension of SAXException to indicate that we've reached the end
   * of the parsing of namespaces for the RDF file.
   */
  private class EndParsingException extends SAXException {
    public EndParsingException(String error) {
      super(error);
    }
  }
}
