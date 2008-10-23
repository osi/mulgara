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
 *   getModel() contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.resolver.lucene;

// Java 2 standard packages
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.transaction.xa.XAResource;

// Log4j
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.BlankNode;
import org.jrdf.graph.Node;
import org.jrdf.graph.Literal;
import org.jrdf.graph.URIReference;

// Locally written packages
import org.mulgara.query.Constraint;
import org.mulgara.query.ConstraintElement;
import org.mulgara.query.LocalNode;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.resolver.spi.AbstractXAResource;
import org.mulgara.resolver.spi.AbstractXAResource.RMInfo;
import org.mulgara.resolver.spi.AbstractXAResource.TxInfo;
import org.mulgara.resolver.spi.EmptyResolution;
import org.mulgara.resolver.spi.GlobalizeException;
import org.mulgara.resolver.spi.LocalizeException;
import org.mulgara.resolver.spi.Resolution;
import org.mulgara.resolver.spi.Resolver;
import org.mulgara.resolver.spi.ResolverFactory;
import org.mulgara.resolver.spi.ResolverException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Statements;
import org.mulgara.resolver.spi.TuplesWrapperResolution;
import org.mulgara.store.tuples.Tuples;
import org.mulgara.store.tuples.TuplesOperations;
import org.mulgara.util.conversion.html.HtmlToTextConverter;

/**
 * Resolves constraints in models defined by static RDF documents.
 *
 * @created 2004-04-01
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/02/22 08:16:13 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class LuceneResolver implements Resolver {

  /** Logger.  */
  private static Logger logger = Logger.getLogger(LuceneResolver.class.getName());

  /**
   * The preallocated node identifying the type of temporary model to create
   * in the {@link #modifyModel} method.
   */
  protected final URI modelTypeURI;

  protected final String directory;

  protected final ResolverSession resolverSession;

  protected final boolean forWrites;

  protected final XAResource xares;

  protected final Map<Long, FullTextStringIndex> indexes = new HashMap<Long, FullTextStringIndex>();

  //
  // Constructors
  //

  /**
   * Construct a resolver.
   *
   * @param modelTypeURI     the URI of the lucene model type
   * @param resolverSession  the session this resolver is associated with
   * @param directory        the directory to use for the lucene indexes
   * @param resolverFactory  the resolver-factory that created us
   * @param forWrites        whether we may be getting writes
   * @throws IllegalArgumentException  if <var>directory</var> is <code>null</code>
   */
  LuceneResolver(URI modelTypeURI, ResolverSession resolverSession, String directory,
                 ResolverFactory resolverFactory, boolean forWrites) {
    if (directory == null) {
      throw new IllegalArgumentException("Null directory in LuceneResolver");
    }

    // Initialize fields
    this.modelTypeURI = modelTypeURI;
    this.directory = directory;
    this.resolverSession = resolverSession;
    this.forWrites = forWrites;
    this.xares = new LuceneXAResource(10, resolverFactory, indexes.values());
  }

  //
  // Methods implementing Resolver
  //

  /**
   * Create a model by treating the <var>model</var> as the {@link URL} of an
   * RDF document and downloading it into the database.
   *
   * @param model  {@inheritDoc}.  In this case, it should be the {@link URL} of
   *   an RDF/XML document.
   * @param modelTypeURI  {@inheritDoc}.  This field is ignored, because URL
   *   models are external.
   */
  public void createModel(long model, URI modelTypeURI)
      throws ResolverException, LocalizeException {
    if (logger.isDebugEnabled()) {
      logger.debug("Create Lucene model " + model);
    }
  }

  public XAResource getXAResource() {
    return xares;
  }

  /**
   * Insert or delete RDF statements in a model at a URL.
   */
  public void modifyModel(long model, Statements statements, boolean occurs)
      throws ResolverException {
    if (logger.isDebugEnabled()) {
      logger.debug("Modify URL model " + model);
    }

    try {
      FullTextStringIndex stringIndex = getFullTextStringIndex(model);

      statements.beforeFirst();
      while (statements.next()) {
        Node subjectNode = resolverSession.globalize(statements.getSubject());

        // Do not insert the triple if it contains a blank node in subject.
        if (subjectNode instanceof BlankNode) {
          if (logger.isInfoEnabled()) {
            logger.info(statements.getSubject() + " is blank node; ignoring Lucene insert.");
          }

          continue;
        }

        Node predicateNode = resolverSession.globalize(statements.getPredicate());
        Node objectNode = resolverSession.globalize(statements.getObject());

        // Get the subject's string value.
        String subject = ((URIReference) subjectNode).getURI().toString();

        // Predicates can only ever be URIReferences.
        String predicate = ((URIReference) predicateNode).getURI().toString();

        if (objectNode instanceof URIReference) {
          URIReference objectURI = (URIReference) objectNode;
          String resource = objectURI.getURI().toString();

          try {
            // Connect to the resource's content
            URLConnection connection = objectURI.getURI().toURL().
                openConnection();
            String contentType = connection.getContentType();

            if (logger.isDebugEnabled()) {
              logger.debug("Content type of resource is " + contentType);
            }

            MimeType contentMimeType;

            try {
              contentMimeType = new MimeType(contentType);
            } catch (MimeTypeParseException e) {
              logger.warn("\"" + contentType + "\" didn't parse as MIME type",
                  e);
              try {
                contentMimeType = new MimeType("content", "unknown");
              } catch (MimeTypeParseException em) {
                throw new ResolverException("Failed to create mime-type", em);
              }
            }

            assert contentMimeType != null;

            // If no character encoding is specified, guess at Latin-1
            String charSet = contentMimeType.getParameter("charset");
            if (charSet == null) {
              charSet = "ISO8859-1";
            }

            assert charSet != null;

            // Get the content, performing appropriate character encoding
            Reader reader = new InputStreamReader(connection.getInputStream(), charSet);

            // Add a filter if the content type is text/html, to strip out
            // HTML keywords that will clutter the index
            try {
              if (contentMimeType.match(new MimeType("text", "html"))) {
                reader = HtmlToTextConverter.convert(reader);
              }
            } catch (MimeTypeParseException em) {
              throw new ResolverException("Failed to create mime-type", em);
            }

            // Assert or deny the statement in the Lucene model
            if (occurs) {
              if (logger.isDebugEnabled()) {
                logger.debug("Inserting " + subject + " " + predicate + " " +
                    resource);
              }

              if (!stringIndex.add(subject, predicate, resource, reader)) {
                logger.warn("Unable to add {" + subject + ", " + predicate +
                    ", " +
                    resource + "} to full text string index");
              }
            } else {
              if (logger.isDebugEnabled()) {
                logger.debug("Deleting " + subject + " " + predicate + " " +
                    resource);
              }

              if (!stringIndex.remove(subject, predicate, resource)) {
                logger.warn("Unable to remove {" + subject + ", " + predicate +
                    ", " + resource + "} from full text string index");
              }
            }
          } catch (MalformedURLException e) {
            logger.info(resource + " is not a URL; ignoring Lucene insert");
          } catch (IOException e) {
            throw new ResolverException("Can't obtain content of " + resource, e);
          } catch (org.mulgara.util.conversion.html.ParseException e) {
            throw new ResolverException("Couldn't parse content of " + resource, e);
          } catch (FullTextStringIndexException e) {
            throw new ResolverException("Unable to modify full text index", e);
          }
        } else if (objectNode instanceof Literal) {
          Literal objectLiteral = (Literal) objectNode;
          String literal = objectLiteral.getLexicalForm();

          // Insert the statement into the text index
          try {
            if (occurs) {
              if (logger.isDebugEnabled()) {
                logger.debug("Inserting " + subject + " " + predicate + " " + literal);
              }

              if (!stringIndex.add(subject, predicate, literal)) {
                logger.warn("Unable to add {" + subject + ", " + predicate +
                    ", " + literal + "} to full text string index");
              }
            } else {
              if (logger.isDebugEnabled()) {
                logger.debug("Deleting " + subject + " " + predicate + " " + literal);
              }

              if (!stringIndex.remove(subject, predicate, literal)) {
                logger.warn("Unable to remove {" + subject + ", " + predicate +
                    ", " + literal + "} from full text string index");
              }
            }
          } catch (FullTextStringIndexException e) {
            throw new ResolverException("Unable to add '" + literal +
                "' to full text string index", e);
          }
        } else {
          if (logger.isInfoEnabled()) {
            logger.info(objectNode + " is blank node; ignoring Lucene insert.");
          }
        }
      }
    } catch (TuplesException et) {
      throw new ResolverException("Error fetching statements", et);
    } catch (GlobalizeException eg) {
      throw new ResolverException("Error localizing statements", eg);
    } catch (FullTextStringIndexException ef) {
      throw new ResolverException("Error in string index", ef);
    }
  }

  /**
   * Remove the cached model containing the contents of a URL.
   */
  public void removeModel(long model) throws ResolverException {
    if (logger.isDebugEnabled()) {
      logger.debug("Removing full-text model " + model);
    }

    try {
      getFullTextStringIndex(model).removeAll();
    } catch (FullTextStringIndexException ef) {
      throw new ResolverException("Query failed against string index", ef);
    }
  }

  /**
   * Resolve a constraint against an RDF/XML document.
   *
   * Resolution is by filtration of a URL stream, and thus very slow.
   */
  public Resolution resolve(Constraint constraint) throws QueryException {
    if (logger.isDebugEnabled()) {
      logger.debug("Resolve " + constraint);
    }

    ConstraintElement modelElement = constraint.getModel();
    if (modelElement instanceof Variable) {
      if (logger.isDebugEnabled()) logger.debug("Ignoring solutions for " + constraint);
      return new EmptyResolution(constraint, false);
    }
    else if (!(modelElement instanceof LocalNode)) {
      throw new QueryException("Failed to localize Lucene Model before resolution " + constraint);
    }

    try {
      FullTextStringIndex stringIndex = getFullTextStringIndex(((LocalNode)modelElement).getValue());
      Tuples tmpTuples = new FullTextStringIndexTuples(stringIndex, constraint, resolverSession);
      Tuples tuples = TuplesOperations.sort(tmpTuples);
      tmpTuples.close();

      return new TuplesWrapperResolution(tuples, constraint);
    } catch (TuplesException te) {
      throw new QueryException("Failed to sort tuples and close", te);
    } catch (FullTextStringIndexException ef) {
      throw new QueryException("Query failed against string index", ef);
    }
  }

  private FullTextStringIndex getFullTextStringIndex(long model) throws FullTextStringIndexException {
    FullTextStringIndex index = indexes.get(model);
    if (index == null) {
      index = new FullTextStringIndex(new File(directory, Long.toString(model)).toString(), "gn" + model, forWrites);
      indexes.put(model, index);
    }
    return index;
  }

  public void abort() {
    try {
      closeIndexes(indexes.values(), false);
    } catch (Exception e) {
      logger.error("Error closing fulltext index", e);
    }
  }

  private static void closeIndexes(Collection<FullTextStringIndex> indexes, boolean commit)
      throws Exception {
    Exception exc = null;

    for (FullTextStringIndex index : indexes) {
      try {
        if (commit) {
          index.optimize();
          index.commit();
        } else {
          index.rollback();
        }
      } catch (Exception e) {
        if (exc == null)
          exc = e;
        else
          logger.error("Error rolling back fulltext index", e);
      } finally {
        try {
          index.close();
        } catch (Exception e) {
          if (exc == null)
            exc = e;
          else
            logger.error("Error closing fulltext index", e);
        }
      }
    }

    if (exc != null)
      throw exc;
  }


  /**
   * An XAResource to manage the lucene indexes.
   */
  private static class LuceneXAResource
      extends AbstractXAResource<RMInfo<LuceneXAResource.LuceneTxInfo>,LuceneXAResource.LuceneTxInfo> {
    private final Collection<FullTextStringIndex> indexes;

    /**
     * Construct a {@link LuceneXAResource} with a specified transaction timeout.
     *
     * @param transactionTimeout transaction timeout period, in seconds
     * @param resolverFactory    the resolver-factory we belong to
     * @param indexes            the list of lucene indexes
     */
    public LuceneXAResource(int transactionTimeout, ResolverFactory resolverFactory, Collection<FullTextStringIndex> indexes) {
      super(transactionTimeout, resolverFactory);
      this.indexes = indexes;
    }

    protected RMInfo<LuceneTxInfo> newResourceManager() {
      return new RMInfo<LuceneTxInfo>();
    }

    protected LuceneTxInfo newTransactionInfo() {
      LuceneTxInfo txInfo = new LuceneTxInfo();
      txInfo.indexes = indexes;
      return txInfo;
    }

    //
    // Methods implementing XAResource
    //

    protected void doStart(LuceneTxInfo tx, int flags, boolean isNew) {
    }

    protected void doEnd(LuceneTxInfo tx, int flags) {
    }

    protected int doPrepare(LuceneTxInfo tx) throws Exception {
      for (FullTextStringIndex index : tx.indexes)
        index.prepare();
      return XA_OK;
    }

    protected void doCommit(LuceneTxInfo tx) throws Exception {
      closeIndexes(tx.indexes, true);
    }

    protected void doRollback(LuceneTxInfo tx) throws Exception {
      closeIndexes(tx.indexes, false);
    }

    protected void doForget(LuceneTxInfo tx) {
    }


    static class LuceneTxInfo extends TxInfo {
      public Collection<FullTextStringIndex> indexes;
    }
  }
}
