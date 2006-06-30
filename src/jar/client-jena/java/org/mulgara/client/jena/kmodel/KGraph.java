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

package org.mulgara.client.jena.kmodel;

//Java 2 standard packages
import java.lang.ref.*;
import java.net.*;
import java.util.*;
import java.io.*;

//Apache packages
import org.apache.log4j.Logger;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.*;

//Kowari packages
import org.kowari.itql.*;
import org.kowari.itql.lexer.*;
import org.kowari.itql.parser.*;
import org.kowari.query.*;
import org.kowari.query.rdf.Tucana;
import org.kowari.server.*;

//JRDF packages


/**
 * A Jena Graph backed by a Kowari triplestore.
 *
 * <p>An instance of this class can only be obtained via a KModel.</p>
 *
 * @created 2001-08-16
 *
 * @author Chris Wilper
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/02/02 21:12:06 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2003 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class KGraph
    extends GraphBase
    implements Graph {

  /** Logger for this class  */
  private final static Logger log = Logger.getLogger(KGraph.class.getName());

  /** The model that this Graph represents */
  private URI modelURI;

  /** Text/Lucene Model (if specified) */
  private URI textModelURI;

  /** Session used to communicate with Model */
  private Session session;

  /** Session used to communicate with Text/Lucene Model (if specified) */
  private Session textModelSession;

  /** Parses/executes iTQL queries */
  private ItqlInterpreter itql;

  /** For inserting multiple statements in a single transaction */
  private TransactionHandler transHandler;

  /** For inserting mulitple Statements */
  private BulkUpdateHandler bulkUpdater;

  /** Used to hold WeakReferences to Closableiterators (for closing) */
  private ReferenceQueue iterRefQueue;

  /** List of ClosableIterators */
  private List iters;

  /** Indicates that the graph has been closed */
  private Throwable closed = null;

  /** What this graph can do */
  private static Capabilities capabilities = new KCapabilities();

  /** New line character(s) */
  private static final String EOL = System.getProperty("line.separator");

  /** special <tks:is> predicate */
  private static final String TKS_IS = "<" + Tucana.NAMESPACE + "is> " ;

  /**
   * Construct a KGraph against the given Kowari model.
   *
   * <p>If <code>textModelURI</code> and <code>textModelSession</code> are
   * non-null, the KGraph will keep that model's literals up-to-date with the
   * regular model's.</p>
   *
   * @see org.mulgara.client.jena.kmodel.KModel#getInstance(URI, URI)
   * @see org.mulgara.client.jena.kmodel.KModel#getInstance(URI, URI, URI)
   * @see org.mulgara.client.jena.kmodel.KModel#getGraph()
   *
   * @param modelURI URI
   * @param modelSession Session
   * @param textModelURI URI
   * @param textModelSession Session
   * @param itql ItqlInterpreter
   */
  protected KGraph(URI modelURI, Session modelSession, URI textModelURI,
                   Session textModelSession, ItqlInterpreter itql) {

    //initialize members
    this.modelURI = modelURI;
    this.session = modelSession;
    this.textModelURI = textModelURI;
    this.textModelSession = textModelSession;
    this.itql = itql;

    //instantiate members
    this.iters = new ArrayList();
    this.iterRefQueue = new ReferenceQueue();
    this.transHandler = new KTransactionHandler(textModelSession);
    this.bulkUpdater = new KBulkUpdateHandler(this);
  }

  /**
   * Returns the BulkUpdateHandler for inserting multiple statements.
   *
   * @return BulkUpdateHandler
   */
  public BulkUpdateHandler getBulkUpdateHandler() {

    return bulkUpdater;
  }

  /**
   * Returns a discription of the actions this model is capable of.
   *
   * @return Capabilities
   */
  public Capabilities getCapabilities() {

    return capabilities;
  }

  /**
   * Returns an Iterator containing the query results.
   *
   * @param match TripleMatch
   * @throws JenaException
   * @return ExtendedIterator
   */
  public ExtendedIterator find(TripleMatch match) throws JenaException {

//    try {
//
//      //convert to JRDF Triple and call find on the session
//      org.jrdf.graph.Triple triple = JRDFUtil.convert(match.asTriple());
//
//      Answer answer = session.find(this.modelURI, triple);
//
//      //wrap the result in an iterator and keep a reference (for closing)
//      KTripleClosableIterator iter = new KTripleClosableIterator(answer, this);
//      iters.add(new WeakReference(iter, iterRefQueue));
//
//      return new TripleMatchIterator(match.asTriple(), iter);
//    }
//    catch (QueryException queryException) {
//
//      throw new JenaException("Failed to find Triple.", queryException);
//    }

    return doQuery(getQueryText(match), match.asTriple());
  }

  /**
   * Get an ExtendedIterator for the results of the given itql query.
   *
   * @param itqlQuery String
   * @param triple Triple
   * @throws JenaException
   * @return ExtendedIterator
   */
  private ExtendedIterator doQuery(String itqlQuery, Triple triple)
      throws JenaException {

    if (log.isDebugEnabled()) {

      log.debug("Querying: " + itqlQuery);
    }

    try {

      //execute the query
      Answer result = this.session.query(this.itql.parseQuery(itqlQuery));

      //wrap the result in an iterator and keep a reference (for closing)
      KTripleClosableIterator iter = new KTripleClosableIterator(result, this);
      iters.add(new WeakReference(iter, iterRefQueue));

      return new TripleMatchIterator(triple, iter);
    }
    catch (Exception exception) {

      throw new JenaException("Cannot perform query: " + itqlQuery, exception);
    }
  }

  /**
   * Converts a TripleMatch to an iTQL Query.
   *
   * <p> In the form of:
   *
   * <code>
   *   select $s $p $o
   *   from <modelURI>
   *   where $s $p $o
   *   and $s <tks:is> <subjectNode>
   *   and $p <tks:is> <predicateNode>
   *   and $o <tks:is> <objectNode> ;
   * </code>
   *
   * @param match TripleMatch
   * @throws JenaException
   * @return String
   */
  private String getQueryText(TripleMatch match) throws JenaException {

    //return value
    StringBuffer query = new StringBuffer();

    //select all from model
    query.append("select $s $p $o" + EOL);
    query.append("from <" + this.modelURI.toString() + ">" + EOL);
    query.append("where $s $p $o" + EOL);

    //constraints
    query.append(getConstraint('s', match.getMatchSubject()) + EOL);
    query.append(getConstraint('p', match.getMatchPredicate()) + EOL);
    query.append(getConstraint('o', match.getMatchObject()));
    query.append(';');

    return query.toString();
  }

  /**
   * Returns a constraint for a given Node/Variable binding.
   *
   * <pre>
   *   and $var <tks:is> <Node>
   * </pre>
   *
   * @param var char
   * @param node Node
   * @throws JenaException
   * @return String
   */
  private String getConstraint(char var, Node node) throws JenaException {

    //null nodes return empty string
    if (node == null || node == Node.ANY || node == Node.NULL) {

      return "";
    }
    else {

      StringBuffer constraint = new StringBuffer();
      constraint.append("and $");
      constraint.append(var);
      constraint.append(TKS_IS);

      if (node.isURI()) {

        // and $var <tks:is> <NodeURI>
        constraint.append('<');
        constraint.append(node.toString());
        constraint.append(">");
      }
      else if (node.isLiteral()) {

        // and $var <tks:is> 'literal'
        constraint.append('\'');
        LiteralLabel lit = node.getLiteral();
        String lang = lit.language();
        String typeURI = lit.getDatatypeURI();
        constraint.append(lit.getLexicalForm());
        constraint.append('\'');

        if (lang != null && !lang.equals("")) {

          // and $var <tks:is> 'literal'@language
          constraint.append('@');
          constraint.append(lang);
        }
        else if (typeURI != null && !typeURI.equals("")) {

          // and $var <tks:is> 'literal'^^<datatypeURI>
          constraint.append("^^<");
          constraint.append(typeURI);
          constraint.append(">");
        }
      }
      else if (node instanceof Node_Blank) {

        //treat as an URI
        constraint.append('<');
        constraint.append("anon:" + node);
        constraint.append(">");
      }
      else {

        throw new JenaException("Node is invalid. Node must be either: null, " +
                                "Node.NULL, Node.ANY, an URI or a Literal. " +
                                "Node: " + node);
      }

      return constraint.toString();
    }
  }

  /**
   * Returns the TransactionHandler used to insert multiple statements in one
   * transaction.
   *
   * @return TransactionHandler
   */
  public TransactionHandler getTransactionHandler() {

    return this.transHandler;
  }

  /**
   * Add one triple.
   *
   * @param triple Triple
   * @throws JenaException
   */
  public void performAdd(Triple triple) throws JenaException {

    this.performAdd(new Triple [] {triple});
  }

  /**
   * Bulk add. Used by KBulkUpdateHandler.
   *
   * @param triples Triple[]
   * @throws JenaException
   */
  protected void performAdd(Triple[] triples) throws JenaException {

    try {

      HashSet set = new HashSet();
      HashSet literalSet = new HashSet();

      //add each Triple to the Set
      for (int i = 0; i < triples.length; i++) {

        org.jrdf.graph.Triple jrdfTriple = JRDFUtil.convert(triples[i]);
        set.add(JRDFUtil.convert(triples[i]));

        //add to text set (if text Model specified)
        if (textModelSession != null
            && triples[i].getObject().isLiteral()) {

          literalSet.add(jrdfTriple);
        }
      }

      //add statements to model
      session.insert(this.modelURI, set);

      //add to text model if statements were added to literalSet
      if ((textModelSession != null)
          && (literalSet.size() > 0)) {

        textModelSession.insert(textModelURI, literalSet);
      }
    }
    catch (QueryException queryException) {

      throw new JenaException("Can't insert statements.", queryException);
    }
  }

  /**
   * Delete one triple.
   *
   * @param triple Triple
   * @throws JenaException
   */
  public void performDelete(Triple triple) throws JenaException {

    this.performDelete(new Triple[] {triple});
  }

  /**
   * Bulk delete. Used by KBulkUpdateHandler.
   *
   * @param triples Triple[]
   * @throws JenaException
   */
  protected void performDelete(Triple[] triples) throws JenaException {

    try {

      HashSet set = new HashSet();
      HashSet literalSet = new HashSet();

      //convert each triple and add to the set
      for (int i = 0; i < triples.length; i++) {

        org.jrdf.graph.Triple jrdfTriple = JRDFUtil.convert(triples[i]);
        set.add(JRDFUtil.convert(triples[i]));

        //add to text set (if text Model specified)
        if (textModelSession != null
            && triples[i].getObject().isLiteral()) {

          literalSet.add(jrdfTriple);
        }
      }

      //delete statements from model
      session.delete(this.modelURI, set);

      //delete from text model if statements were added to literalSet
      if ((textModelSession != null)
          && (literalSet.size() > 0)) {

        textModelSession.delete(textModelURI, literalSet);
      }
    }
    catch (QueryException queryException) {

      throw new JenaException("Can't delete statements.", queryException);
    }
  }

  /**
   * Return the number of statements in the graph.
   *
   * @return int
   */
  public int size() {

    String query = null;

    try {

      //value to be returned
      int size = 0;

      //select all
      query = "select $s $p $o " +
          "from <" + this.modelURI + "> " +
          "where $s $p $o ;";
      Answer answer = session.query(itql.parseQuery(query));

      //count rows
      size = (int) answer.getRowCount();
      answer.close();

      return size;
    }
    catch (TuplesException tuplesException) {

      throw new JenaException("Could not determine size.", tuplesException);
    }
    catch (QueryException queryException) {

      throw new JenaException("Could not determine size. Could not execute " +
                              "query: " + query, queryException);
    }
    catch (LexerException lexerException) {

      throw new JenaException("Could not determine size. Could not parse " +
                              "query: " + query, lexerException);
    }
    catch (ParserException parserException) {

      throw new JenaException("Could not determine size. Could not parse " +
                              "query: " + query, parserException);
    }
    catch (IOException ioException) {

      throw new JenaException("Could not determine size. Could not parse " +
                              "query: " + query, ioException);
    }
  }

  /**
   * Callback to notify the KGraph that we no longer need to hold a ref.
   *
   * @param iterator ClosableIterator
   */
  protected void iteratorClosed(ClosableIterator iterator) {

    //remove iterator from Set
    if (iters.contains(iterator)) {

      iters.remove(iterator);
    }
  }

  /**
   * Ensure all iterators are closed, then close the session(s).
   *
   * @throws JenaException
   */
  public void close() throws JenaException {

    //has the Graph been closed already?
    if (closed != null) {

      closed = new Throwable("First closed.");

      try {

        // ensure all iterators are closed...
        WeakReference reference = null;
        ClosableIterator iterator = null;
        for (int i = 0; i < iters.size(); i++) {

          reference = (WeakReference) iters.remove(0);
          iterator = (ClosableIterator) reference.get();

          //close the iterator (if it hasn't been collected)
          if (iterator != null) {

            iterator.close();
          }
        }

        // close the session
        session.close();

        // close text session (if used)
        if (textModelSession != null) {

          textModelSession.close();
        }
      }
      catch (QueryException queryException) {

        throw new JenaException("Could not close Session.", queryException);
      }
    }
  }

}
