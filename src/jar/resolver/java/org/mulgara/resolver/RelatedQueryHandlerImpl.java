package org.mulgara.resolver;

// Java 2 standard packages
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.*;

// Logging classes
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.Node;

// Locally written packages
import org.mulgara.query.*;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.server.*;
import org.mulgara.store.*;
import org.mulgara.util.ResultSetRow;
import org.mulgara.util.TestResultSet;

/**
 * All related queries are found here.
 *
 * @created 2003-10-27
 *
 * @author David Makepeace
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:24 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 */
public class RelatedQueryHandlerImpl extends RelatedQueryHandler {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(RelatedQueryHandlerImpl.class.getName());

  /*
   * Create a new related queries.
   *
   * @param newSession the session being used to do the querying.
   */
  public RelatedQueryHandlerImpl(Session newSession) {
    super(newSession);
  }

  /**
   * Find related RDF nodes.
   *
   * @param baseNode the base RDF node.
   * @param queries the queries to perform
   * @param maxRelated the maximum number of rows to return
   * @param minScore related nodes must have at least this score
   * @return a non-<code>null</code> answer with two columns. The first column
   *      is the RDF node and the second column is the score.
   * @throws QueryException if any of the <var>queries</var> can't be processed
   */
  public synchronized Answer related(Node baseNode, Query[] queries,
      int maxRelated, double minScore) throws QueryException {
    throw new QueryException("Related queries are not supported in Kowari");
  }

  /**
   * Show how two nodes are related.
   *
   * @param baseNode the base RDF node.
   * @param relatedNode the related RDF node.
   * @param queries the queries to perform.
   * @param nrPColumns the number of columns in the query results which identify
   *      a category of arc linking the two RDF nodes. The columns used for this
   *      purpose start at column 2 (the second column) of the result.
   * @return a non-<code>null</code> answer with two or more columns. The number
   *      of columns is the value of the <code>nrColumns</code> parameter plus
   *      one. The initial <code>nrPColumns</code> columns identify a particular
   *      category of arc linking the two RDF nodes and the last column is the
   *      score.
   * @throws QueryException if any of the <var>queries</var> can't be processed
   */
  public synchronized Answer howRelated(Node baseNode, Node relatedNode,
      Query[] queries, int nrPColumns) throws QueryException {
    throw new QueryException("Related queries are not supported in Kowari");
  }
}
