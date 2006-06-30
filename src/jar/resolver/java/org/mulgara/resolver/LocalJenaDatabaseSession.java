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

package org.mulgara.resolver;

// Java 2 standard packages
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

// Java 2 enterprise packages
import javax.transaction.TransactionManager;

// Third party packages
import org.apache.log4j.Logger;

// JRDF classes
import org.jrdf.graph.*;

// Jena Classes
import com.hp.hpl.jena.graph.Node_Variable;

// Local packages
import org.kowari.jena.*;
import org.mulgara.content.ContentHandlerManager;
import org.mulgara.jrdf.*;
import org.mulgara.query.*;
import org.mulgara.query.rdf.*;
import org.mulgara.resolver.spi.*;
import org.mulgara.server.*;
import org.mulgara.store.statement.StatementStore;

/**
 * A JRDF database session.
 *
 * @created 2004-10-26
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.12 $
 *
 * @modified $Date: 2005/05/19 08:43:59 $ by $Author: raboczi $
 *
 * @maintenanceAuthor $Author: raboczi $
 *
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class LocalJenaDatabaseSession extends LocalJRDFDatabaseSession
    implements LocalJenaSession {

  /**
   * Logger.
   */
  private static final Logger logger =
      Logger.getLogger(LocalJenaDatabaseSession.class.getName());

  /**
   * Factory use to map Jena blank nodes to JRDF blank nodes.
   */
  private JenaFactory jenaFactory;

  /**
   * Iterator handler.
   */
  private IteratorHandler iteratorHandler = new IteratorHandlerImpl();


  /**
   * Construct a database session.
   *
   * @param transactionManager  the source of transactions for this session,
   *   never <code>null</code>
   * @param securityAdapterList  {@link List} of {@link SecurityAdapter}s to be
   *   consulted before permitting operations, never <code>null</code>
   * @param symbolicTransformationList  {@link List} of
   *   {@link SymbolicTransformation}s to apply
   * @param resolverSessionFactory  source of {@link ResolverSessionFactory}s,
   *   never <code>null</code>
   * @param systemResolverFactory  Source of {@link SystemResolver}s to manage
   *   persistent models, for instance the system model (<code>#</code>); never
   *   <code>null</code>
   * @param temporaryResolverFactory  Source of {@link Resolver}s to manage
   *   models which only last the duration of a transaction, for instance the
   *   contents of external RDF/XML documents; never <code>null</code>
   * @param resolverFactoryList  the list of registered {@link ResolverFactory}
   *   instances to use for constraint resolution, never <code>null</code>
   * @param externalResolverFactoryMap  map from URL protocol {@link String}s
   *   to {@link ResolverFactory} instances for models accessed via that
   *   protocol, never <code>null</code>
   * @param internalResolverFactoryMap  map from model type {@link LocalNode}s
   *   to {@link ResolverFactory} instances for that model type, never
   *   <code>null</code>
   * @param metadata  even more parameters from the parent {@link Database},
   *   never <code>null</code>
   * @param contentHandlers contains the list of valid registered content handles
   *   never <code>null</code>
   * @param relatedQueryHandler the name of the class that implements
   *   {@link RelatedQueryHandler}
   * @throws IllegalArgumentException if any argument is <code>null</code>
   */
  LocalJenaDatabaseSession(TransactionManager transactionManager,
      List securityAdapterList, List symbolicTransformationList,
      ResolverSessionFactory resolverSessionFactory,
      SystemResolverFactory systemResolverFactory,
      ResolverFactory temporaryResolverFactory, List resolverFactoryList,
      Map externalResolverFactoryMap, Map internalResolverFactoryMap,
      DatabaseMetadata metadata, ContentHandlerManager contentHandlers,
      Set cachedResolverFactorySet, String relatedQueryHandler,
      URI temporaryModelTypeURI)
      throws ResolverFactoryException {
    super(transactionManager, securityAdapterList, symbolicTransformationList,
        resolverSessionFactory,
        systemResolverFactory, temporaryResolverFactory, resolverFactoryList,
        externalResolverFactoryMap, internalResolverFactoryMap, metadata,
        contentHandlers, cachedResolverFactorySet, relatedQueryHandler,
        temporaryModelTypeURI);

    jenaFactory = new JenaFactoryImpl();
    jrdfFactory = new JRDFFactoryImpl(jenaFactory);
    jenaFactory.setJrdfFactory(getJRDFFactory());
  }

  public void close() throws QueryException {
    iteratorHandler.close();
    super.close();
  }

  /**
   * @throws IllegalArgumentException if the graph given is not a Kowari graph.
   */
  public com.hp.hpl.jena.util.iterator.ClosableIterator find(URI modelURI,
      com.hp.hpl.jena.graph.Node subject, com.hp.hpl.jena.graph.Node predicate,
      com.hp.hpl.jena.graph.Node object)
      throws QueryException, IllegalArgumentException {

    try {
      JRDFGraph tmpGraph = new JRDFGraph(this, modelURI);

      SubjectNode subjectNode;
      PredicateNode predicateNode;
      ObjectNode objectNode;

      // Only create a new blank node if not already in JRDF factory
      if (subject.isBlank() && !getJRDFFactory().hasNode(subject)) {
        subjectNode = new BlankNodeImpl();
      }
      else {
        subjectNode = getJRDFFactory().convertNodeToSubject(
            tmpGraph, subject);
      }

      predicateNode = getJRDFFactory().convertNodeToPredicate(
          tmpGraph, predicate);

      // Only create a new blank node if not already in JRDF factory
      if (object.isBlank() && !getJRDFFactory().hasNode(object)) {
        objectNode = new BlankNodeImpl();
      }
      else {
        objectNode = getJRDFFactory().convertNodeToObject(
            tmpGraph, object);
      }

      Answer answer = find(modelURI, subjectNode, predicateNode, objectNode);

      com.hp.hpl.jena.graph.Triple triple =
          new com.hp.hpl.jena.graph.Triple(subject, predicate, object);

      org.kowari.jena.AnswerClosableIteratorImpl iter =
          new org.kowari.jena.AnswerClosableIteratorImpl(answer, triple,
          modelURI, jenaFactory, iteratorHandler);
       return iter;
    }
    catch (URISyntaxException use) {
      throw new QueryException("Failed to find nodes", use);
    }
    catch (GraphElementFactoryException gefe) {
      throw new QueryException("Failed to find nodes", gefe);
    }
    catch (GraphException ge) {
      throw new QueryException("Failed to find nodes", ge);
    }
  }

  public boolean contains(URI modelURI, com.hp.hpl.jena.graph.Node subject,
      com.hp.hpl.jena.graph.Node predicate, com.hp.hpl.jena.graph.Node object)
      throws QueryException {
    try {
      JRDFGraph tmpGraph = new JRDFGraph(this, modelURI);

      SubjectNode subjectNode;
      PredicateNode predicateNode;
      ObjectNode objectNode;

      // Only create a new blank node if not already in JRDF factory
      if (subject.isBlank() && !getJRDFFactory().hasNode(subject)) {
        subjectNode = new BlankNodeImpl();
      }
      else {
        subjectNode = getJRDFFactory().convertNodeToSubject(
            tmpGraph, subject);
      }

      predicateNode = getJRDFFactory().convertNodeToPredicate(
          tmpGraph, predicate);

      // Only create a new blank node if not already in JRDF factory
      if (object.isBlank() && !getJRDFFactory().hasNode(object)) {
        objectNode = new BlankNodeImpl();
      }
      else {
        objectNode = getJRDFFactory().convertNodeToObject(
            tmpGraph, object);
      }

      return contains(modelURI, subjectNode, predicateNode, objectNode);
    }
    catch (URISyntaxException use) {
      throw new QueryException("Failed to find nodes", use);
    }
    catch (GraphElementFactoryException gefe) {
      throw new QueryException("Failed to find nodes", gefe);
    }
    catch (GraphException ge) {
      throw new QueryException("Failed to find nodes", ge);
    }
  }

  public void insert(URI modelURI,
      com.hp.hpl.jena.graph.Node subject, com.hp.hpl.jena.graph.Node predicate,
      com.hp.hpl.jena.graph.Node object) throws QueryException {
    startTransactionalOperation(true);
    try {
      JRDFGraph tmpGraph = new JRDFGraph(this, modelURI);
      modify(modelURI, tmpGraph, subject, predicate, object, true);
      return;
    }
    catch (Throwable e) {
      rollbackTransactionalBlock(e);
    }
    finally {
      finishTransactionalOperation("Could not commit insert");
    }
  }

  public void insert(URI modelURI, com.hp.hpl.jena.graph.Triple[] triples)
      throws QueryException {
    startTransactionalOperation(true);
    try {
      JRDFGraph tmpGraph = new JRDFGraph(this, modelURI);
      for (int index = 0; index < triples.length; index++) {
        modify(modelURI, tmpGraph, triples[index].getSubject(),
            triples[index].getPredicate(), triples[index].getObject(), true);
      }
      return;
    }
    catch (Throwable e) {
      rollbackTransactionalBlock(e);
    }
    finally {
      finishTransactionalOperation("Could not commit insert");
    }
  }

  /**
   * Converts Jena objects to JRDF objects and inserts them into the graph.
   *
   * @param modelURI the URI of the model.
   * @param tmpGraph the JRDF graph to use to localize objects.
   * @param subject the subject node to insert.
   * @param predicate the predicate node to insert.
   * @param object the object node to insert
   * @param add true if we're adding the statement, false to remove.
   * @throws Throwable if anything goes wrong.
   */
  private void modify(URI modelURI, JRDFGraph tmpGraph,
      com.hp.hpl.jena.graph.Node subject, com.hp.hpl.jena.graph.Node predicate,
      com.hp.hpl.jena.graph.Node object, boolean add) throws Throwable {

    SubjectNode subjectNode;
    PredicateNode predicateNode;
    ObjectNode objectNode;

    try {
      // Suspend the transaction in case JRDF needs it.
      suspendTransactionalBlock();

      // Convert Jena objects to JRDF objects
      subjectNode = getJRDFFactory().convertNodeToSubject(
          tmpGraph, subject);
      predicateNode = getJRDFFactory().convertNodeToPredicate(
          tmpGraph, predicate);
      objectNode = getJRDFFactory().convertNodeToObject(
          tmpGraph, object);
    }
    finally {

      // Resume the transaction.
      resumeTransactionalBlock();
    }

    Set statements = Collections.singleton(tmpGraph.getElementFactory().
        createTriple(subjectNode, predicateNode, objectNode));

    Statements wrapped = new TripleSetWrapperStatements(statements,
        systemResolver, TripleSetWrapperStatements.PERSIST);
    doModify(systemResolver, modelURI, wrapped, add);
  }

  public void delete(URI modelURI, com.hp.hpl.jena.graph.Node subject,
      com.hp.hpl.jena.graph.Node predicate, com.hp.hpl.jena.graph.Node object)
      throws QueryException {
    startTransactionalOperation(true);
    try {
      JRDFGraph tmpGraph = new JRDFGraph(this, modelURI);
      modify(modelURI, tmpGraph, subject, predicate, object, false);
      return;
    }
    catch (Throwable e) {
      rollbackTransactionalBlock(e);
    }
    finally {
      finishTransactionalOperation("Could not commit insert");
    }
  }

  public void delete(URI modelURI, com.hp.hpl.jena.graph.Triple[] triples)
      throws QueryException {
    startTransactionalOperation(true);
    try {
      JRDFGraph tmpGraph = new JRDFGraph(this, modelURI);
      for (int index = 0; index < triples.length; index++) {
        modify(modelURI, tmpGraph, triples[index].getSubject(),
            triples[index].getPredicate(), triples[index].getObject(), false);
      }
      return;
    }
    catch (Throwable e) {
      rollbackTransactionalBlock(e);
    }
    finally {
      finishTransactionalOperation("Could not commit insert");
    }
  }

  public long getNumberOfStatements(URI modelURI) throws QueryException {
    return getNumberOfTriples(modelURI);
  }

  public JenaFactory getJenaFactory() {
    return jenaFactory;
  }

  /**
   * @throws IllegalArgumentException if the graph given is not a Kowari graph.
   */
  public com.hp.hpl.jena.util.iterator.ClosableIterator findUniqueValues(
      URI modelURI, Node_Variable column) throws QueryException {

    Variable realColumn = null;
    if (GraphKowari.VARIABLES[0].equals(column)) {
      realColumn = StatementStore.VARIABLES[0];
    }
    else if (GraphKowari.VARIABLES[1].equals(column)) {
      realColumn = StatementStore.VARIABLES[1];
    }
    else if (GraphKowari.VARIABLES[2].equals(column)) {
      realColumn = StatementStore.VARIABLES[2];
    }

    //select $Subject $Predicate $Object
    ConstraintElement[] vars = new ConstraintElement[3];
    vars[0] = (ConstraintElement) StatementStore.VARIABLES[0];
    vars[1] = (ConstraintElement) StatementStore.VARIABLES[1];
    vars[2] = (ConstraintElement) StatementStore.VARIABLES[2];

    //where $Subject $Predicate $Object
    ConstraintImpl varConstraint = new ConstraintImpl(vars[0], vars[1], vars[2]);

    // Replace with tucana:is instead.  This will be much faster.
    //and ... ... ...
    ConstraintElement[] e = new ConstraintElement[3];

    return null;

//    try {
//      e[0] = (subject == null)
//          ? (ConstraintElement) StatementStore.VARIABLES[0]
//          : (ConstraintElement) toValue(subject);
//
//      e[1] = (predicate == null)
//          ? (ConstraintElement) StatementStore.VARIABLES[1]
//          : (ConstraintElement) toValue(predicate);
//
//      e[2] = (object == null)
//          ? (ConstraintElement) StatementStore.VARIABLES[2]
//          : (ConstraintElement) toValue(object);
//
//      ConstraintImpl eConstraint = new ConstraintImpl(e[0], e[1], e[2]);
//
//      return query(new Query(
//          Arrays.asList(vars), // variable list
//          new ModelResource(modelURI), // model expression
//          new ConstraintConjunction(varConstraint, eConstraint), // constraint expr
//          null, // no having
//          Collections.EMPTY_LIST, // no ordering
//          null, // no limit
//          0, // zero offset
//          new UnconstrainedAnswer() // nothing given
//          ));
//    }
//    catch (QueryException qe) {
//      throw new GraphException("Failed to find the given triple pattern in " +
//          " the model.", qe);
//    }
  }
}
