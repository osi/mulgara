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

package org.mulgara.webui.viewer;

import java.io.*;
import java.net.*;

// For result sets
import java.sql.*;
import java.text.DecimalFormat;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.naming.Context;

import org.apache.log4j.*;

import org.jrdf.graph.Literal;
import org.jrdf.graph.URIReference;
import org.jrdf.graph.BlankNode;

import org.enhydra.barracuda.core.comp.*;
import org.enhydra.barracuda.core.event.*;
import org.enhydra.barracuda.core.event.helper.*;
import org.enhydra.barracuda.core.forms.*;
import org.enhydra.barracuda.core.forms.validators.*;
import org.enhydra.barracuda.plankton.data.MapStateMap;
import org.enhydra.barracuda.core.util.dom.*;
import org.enhydra.barracuda.core.util.http.*;
import org.w3c.dom.*;
import org.w3c.dom.html.*;

// Other locally required classes
import org.mulgara.barracuda.dom.util.HtmlTableBuilder;
import org.mulgara.barracuda.gateway.ExceptionHandlerGateway;

// For queries
import org.mulgara.itql.*;

// For answers
import org.mulgara.query.*;
import org.mulgara.query.rdf.*;
import org.mulgara.server.EmbeddedMulgaraServer;

// For displaying exceptions
import org.mulgara.webui.viewer.events.*;
import javax.servlet.ServletException;
import org.enhydra.barracuda.core.event.EventException;
import java.util.List;
import org.w3c.dom.html.HTMLTableElement;

/**
 * Event handlers (both Controller and View) for the Viewer screen.
 *
 * @created 2002-02-15
 *
 * @author Ben Warren
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2004/12/22 05:04:50 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ViewerScreen
    extends DefaultEventGateway {

  /**
   * The logger for this class
   */
  protected static final Logger log = Logger.getLogger(ViewerScreen.class.
      getName());

  /**
   * The number of results per page
   */
  public static final int RESULTS_PER_PAGE = 250;

  /**
   * The directives
   */
  private static Properties properties;

  /**
   * The example query text
   */
  private static List exampleQueryText;

  /**
   * The example query display values
   */
  private static List exampleQueryDisplay;

  /**
   * The path to the mulgara jar file
   */
  private static String jarPath = null;

  /**
   * The machine host name
   */
  private static String hostName = null;

  /**
   * The model URI
   */
  private String modelURI;

  // Event handlers

  /**
   * Listener for GetViewerScreen events
   */
  private ListenerFactory getViewerScreenFactory;

  /**
   * Listener for RenderViewerScreen events
   */
  private ListenerFactory renderViewerScreenFactory;

  /**
   * Listener for ExecuteQuery events
   */
  private ListenerFactory executeQueryFactory;

  /**
   * The template page
   */
  private ViewerScreenHTML templateScreen;

  /**
   * Public constructor
   */
  public ViewerScreen() {

    // Get the path to the mulgara jar
    if (jarPath == null) {

      jarPath = System.getProperty("mulgara.jar.path");

      if (log.isDebugEnabled()) {

        log.debug("Using jar path '" + jarPath + "'");
      }
    }

    // Get the host name
    try {

      // Get the provider from the system property.
      URI uri = new URI(System.getProperty(Context.PROVIDER_URL));

      if (uri.getPort() != 1099) {

        hostName = uri.getHost() + ":" + uri.getPort();
      }
      else {

        hostName = uri.getHost();
      }
    }
    catch (URISyntaxException use) {

      // If this fails get the current host name and assume 1099.
      hostName = EmbeddedMulgaraServer.getResolvedLocalHost();
    }

    // Default to localhost
    if (hostName == null) {

      hostName = "localhost";
    }

    if (log.isDebugEnabled()) {

      log.debug("Host name is set to '" + hostName + "'");
    }

    // Create the event listeners and register handlers for possible events.
    // Get the query screen event
    getViewerScreenFactory =
        new EventForwardingFactory(new RenderViewerScreen());
    specifyLocalEventInterests(getViewerScreenFactory, GetViewerScreen.class);

    // Render the query screen event
    renderViewerScreenFactory =
        new DefaultListenerFactory() {

      public BaseEventListener getInstance() {

        return new RenderViewerScreenHandler();
      }

      public String getListenerID() {

        return getID(RenderViewerScreenHandler.class);
      }
    };
    specifyLocalEventInterests(renderViewerScreenFactory,
        RenderViewerScreen.class);

    // Execute query event
    executeQueryFactory =
        new DefaultListenerFactory() {

      public BaseEventListener getInstance() {

        return new ExecuteQueryHandler();
      }

      public String getListenerID() {

        return getID(ExecuteQueryHandler.class);
      }
    };
    specifyLocalEventInterests(executeQueryFactory, ExecuteQuery.class);

    // Load directives.
    if (properties == null) {

      try {

        properties = new Properties();
        properties.load(this.getClass().getResourceAsStream(
            "ViewerScreen.directives"));
      }

      // Report error to the user and log
      catch (IOException e) {

        ExceptionHandlerGateway.getInstance().registerException(e,
            "Fatal error loading directives file");
      }
    }

    // Load example queries.
    if (exampleQueryText == null) {

      exampleQueryDisplay = new ArrayList();
      exampleQueryText = new ArrayList();

      // Read in the query file.
      try {

        //
        //        Properties queries = new Properties();
        InputStream in =
            this.getClass().getResourceAsStream("ExampleQueries.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line = reader.readLine();
        int lineNum = 0;

        while (line != null) {

          lineNum++;
          line = line.trim();

          // If not a comment or blank line.
          if (! (line.startsWith("#") || (line.length() == 0))) {

            String[] splits = line.split("=");

            // Report error to the user and log
            if (splits.length != 2) {

              String message =
                  "Error on line " + lineNum +
                  ". Should be: 'Display Text = Query Text', but was: '" + line +
                  "'";

              throw new Exception(message);
            }

            // Found ModelURI
            if (splits[0].trim().equals("ModelURI")) {

              modelURI = splits[1].trim();

              modelURI = modelURI.replaceAll("host.name", hostName);
            }
            else {

              exampleQueryDisplay.add(splits[0].trim());

              String queryText = splits[1].trim();

              // If the jar path is set then we are loading data files from
              // inside the jar.
              if (jarPath != null) {

                queryText = queryText.replaceAll("mulgara.jar.path", jarPath);
              }

              queryText = queryText.replaceAll("host.name", hostName);
              exampleQueryText.add(queryText);
            }
          }

          line = reader.readLine();
        }

        // No ModelURI found
        if (modelURI == null) {

          throw new Exception("No ModelURI was found. " +
              "Need line 'ModelURI = model'");
        }
      }

      // Report error to the user and log
      catch (Exception e) {

        ExceptionHandlerGateway.getInstance().registerException(e,
            "Fatal error loading example queries file:");
      }
    }
  }


  /**
   * Close all answers in the result list.
   *
   * @param resultList List The list of query results.
   * @throws TuplesException If the answer can't be closed.
   */
  private void closeAnswers(List resultList)
      throws TuplesException {

    // Populate the result rows
    for (int resultIndex = 0; resultIndex < resultList.size();
        resultIndex++) {

      QueryResult queryResult = (QueryResult) resultList.get(resultIndex);
      Answer answer = queryResult.answer;

      // Got a message
      if (answer != null) {
        answer.close();
      }
    }
  }


  //------------------------------------------------------------
  //             Controller Event Handlers
  //------------------------------------------------------------

  /**
   * Handles SetServer events which occur when the "SetServerButton" is clicked.
   */
  class ExecuteQueryHandler
      extends DefaultBaseEventListener {

    /**
     * Handles SetServer events
     *
     * @param context PARAMETER TO DO
     * @throws EventException EXCEPTION TO DO
     * @throws ServletException EXCEPTION TO DO
     * @throws IOException EXCEPTION TO DO
     */
    public void handleControlEvent(ControlEventContext context)
        throws EventException, ServletException, IOException {

      // Reassign a fresh template page
      templateScreen = new ViewerScreenHTML(true);

      // Unpack the necessary entities from the context
      BaseEvent event = context.getEvent();
      HttpServletRequest req = context.getRequest();
      HttpSession session = req.getSession();

      // Get the query info from the request
      String indexString = req.getParameter(WebUIKeys.RESULT_INDEX);

      String queryString = req.getParameter(WebUIKeys.QUERY_TEXT);

      if (queryString == null) {

        queryString = "";
      }

      String queryResource = req.getParameter(WebUIKeys.QUERY_RESOURCE);

      if (queryResource == null) {

        queryResource = "";
      }

      String queryLiteral = req.getParameter(WebUIKeys.QUERY_LITERAL);

      if (queryLiteral == null) {

        queryLiteral = "";
      }

      String exampleQuery = req.getParameter(WebUIKeys.EXAMPLE_QUERY);

      if (exampleQuery == null) {

        exampleQuery = "";
      }

      String newModelURI = req.getParameter(WebUIKeys.MODEL_URI);

      if (newModelURI == null) {

        newModelURI = modelURI;
      }

      List result = null;

      try {

        // New query
        if (indexString == null) {

          // Select the query to execute
          String queryToExecute;

          // A query on a node from a result URL
          if (queryResource.length() > 0) {

            queryToExecute = buildResourceQuery(newModelURI, queryResource);
          }
          else if (queryLiteral.length() > 0) {

            queryToExecute = buildLiteralQuery(newModelURI, queryLiteral);
          }
          // User clicked the excute query button
          else {

            queryToExecute = queryString;
          }

          session.setAttribute(WebUIKeys.QUERY_TEXT, queryString);
          session.setAttribute(WebUIKeys.QUERY_EXECUTED, queryToExecute);
          session.setAttribute(WebUIKeys.EXAMPLE_QUERY, exampleQuery);
          session.setAttribute(WebUIKeys.MODEL_URI, newModelURI);

          // Ensure the user has an interpreter
          ItqlInterpreterBean interpreter =
              (ItqlInterpreterBean) session.getAttribute(WebUIKeys.ITQL_INT);

          if (interpreter == null) {
            interpreter = new ItqlInterpreterBean();
            session.setAttribute(WebUIKeys.ITQL_INT, interpreter);
          }

          // Get the template table
          HTMLTableElement templateTable = templateScreen.getElementResultTable();

          // Do the query
          double time = System.currentTimeMillis();

          // Do the query
          result =
              executeQuery(queryToExecute, interpreter, templateTable,
              newModelURI);

          time = (System.currentTimeMillis() - time) / 1000;

          DecimalFormat timeFormat = new DecimalFormat();
          timeFormat.setMaximumFractionDigits(3);
          timeFormat.setMinimumFractionDigits(3);
          session.setAttribute(WebUIKeys.RESULT_STATS, timeFormat.format(time));
        }
        // Just viewing next page of a result list
        else {

          HTMLTableElement templateTable = templateScreen.getElementResultTable();
          int resultIndex = Integer.parseInt(indexString);
          result = (List) session.getAttribute(WebUIKeys.RESULT);
          if (result != null) {
            incrementResults(result, resultIndex, templateTable, newModelURI);
          }
        }
      }
      // Set message text to exception text
      catch (Exception e) {

        // Get original cause
        Throwable cause = e.getCause();
        Throwable lastCause = e;

        while (cause != null) {

          lastCause = cause;
          cause = cause.getCause();
        }

        // Try to use message else use the toString
        result = new ArrayList();

        String exceptionMessage = lastCause.getMessage();

        if (exceptionMessage == null) {

          exceptionMessage = lastCause.toString();
        }

        exceptionMessage = "Query Error: " + exceptionMessage;

        QueryResult queryResult = new QueryResult();
        queryResult.actualResult = exceptionMessage;
        result.add(queryResult);

        // Log full stack trace
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.flush();
        stringWriter.flush();

        log.error("Error while processing query: '" + queryString + "' \n" +
            stringWriter.getBuffer().toString());
      }

      // Close answers in previous results
      if (result == null ||
          (result != null && !result.equals(session.getAttribute(WebUIKeys.RESULT)))) {

        List oldResult = (List) session.getAttribute(WebUIKeys.RESULT);

        if (oldResult != null) {
          try {
            closeAnswers(oldResult);
          }
          catch (TuplesException ex) {
            log.warn("Exception while closing answers", ex);
          }
        }
      }

      session.setAttribute(WebUIKeys.RESULT, result);
      session.setAttribute(WebUIKeys.CONTROLLER_FLAG, "true");

      try {

        // Get the query screen - add the current time to make the URL's
        // different. This will allow use of the back button from query to query.
        String eventName = new GetViewerScreen().getEventIDWithExtension();
        throw new ClientSideRedirectException(eventName + "?pageID=" +
            System.currentTimeMillis());
      }
      finally {

        event.setHandled(true);
      }
    }

    /**
     * Execute the query.
     *
     * @param queryString The query to execute.
     * @param interpreter The interpreter to use for the query.
     * @param resultTableTemplate The template table to use.
     * @param modelURI The model URI.
     * @return The result which will be a list of ResultObjects which contain a
     *      String message and the query time or list of HTMLTableElements and
     *      the query time. NOTE: null will be returned if queryString is null.
     * @throws Exception if there is a problem executing or processing the
     *      query.
     */
    private List executeQuery(String queryString,
        ItqlInterpreterBean interpreter, HTMLTableElement resultTableTemplate,
        String modelURI)
        throws Exception {

      // List to put the query result tables in
      List resultList = new ArrayList();

      // There is a query and it is empty
      if ( (queryString != null) && (queryString.length() == 0)) {

        QueryResult finalResult = new QueryResult();
        finalResult.actualResult = "Please enter a query";
        resultList.add(finalResult);

        return resultList;
      }

      // Execute the query
      else if (queryString != null) {

        // result will contain strings or answer objects
        List queryResult = interpreter.executeQueryToList(queryString);

        if (log.isDebugEnabled()) {

          log.debug("Got " + queryResult.size() + " results back");
        }

        Iterator resultIter = (Iterator) queryResult.iterator();

        // Iterate over the results
        while (resultIter.hasNext()) {

          Object resultObject = resultIter.next();

          // Got a message back
          if (resultObject instanceof String) {

            QueryResult finalResult = new QueryResult();
            finalResult.actualResult = resultObject;
            resultList.add(finalResult);

            if (log.isDebugEnabled()) {

              log.debug("Query returned a result message: " + resultObject);
            }
          }

          // Got an answer object
          else {

            if (log.isDebugEnabled()) {

              log.debug("Query returned an answer");
            }

            // Get the result set
            Answer answer = (Answer) resultObject;
            answer.beforeFirst();
            HTMLTableElement resultTable =
                createResultTable(answer, ViewerScreen.RESULTS_PER_PAGE,
                resultTableTemplate, modelURI);

            // Got a result table
            if (resultTable != null) {

              QueryResult finalResult = new QueryResult();
              finalResult.actualResult = resultTable;
              finalResult.answer = answer;

              // Add the table to the result list
              resultList.add(finalResult);
            }

            // Null result
            else {

              QueryResult finalResult = new QueryResult();
              finalResult.actualResult = "No results found";
              resultList.add(finalResult);
            }
          }
        }

        return resultList;
      }

      // Query was null
      else {

        return null;
      }
    }


    /**
     * Execute the query.
     *
     * @param resultList The result list from the original queries.
     * @param resultIndex The index into the result list.
     * @param resultTableTemplate The template table to use.
     * @param modelURI The model URI.
     * @return The result which will be a list of ResultObjects which contain a
     *      String message and the query time or list of HTMLTableElements and
     *      the query time. NOTE: null will be returned if queryString is null.
     * @throws Exception if there is a problem executing or processing the
     *      query.
     */
    private void incrementResults(List resultList,
        int resultIndex, HTMLTableElement resultTableTemplate, String modelURI)
        throws Exception {

      QueryResult queryResult = (QueryResult) resultList.get(resultIndex);

      if (queryResult != null) {

        Answer answer = queryResult.answer;

        if (answer != null) {

          HTMLTableElement resultTable =
              createResultTable(answer, ViewerScreen.RESULTS_PER_PAGE,
              resultTableTemplate, modelURI);

          if (resultTable != null) {
            queryResult.actualResult = resultTable;
          }
          else {
            queryResult.actualResult = "No results found";
          }
        }
        else {
          throw new IllegalStateException("No answer found in result at index "+
              resultIndex);
        }
      }
      else {
        throw new IllegalStateException("No result found at index "+resultIndex);
      }
    }



    /**
     * Create the result table.
     *
     * @param answer The answer to build the table for.
     * @param numResults
     * @param resultTableTemplate The template table.
     * @param modelURI The model being queried.
     * @return RETURNED VALUE TO DO
     * @throws Exception EXCEPTION TO DO
     */
    private HTMLTableElement createResultTable(Answer answer, int numResults,
        HTMLTableElement resultTableTemplate, String modelURI)
        throws Exception {

      if (answer.next()) {

        try {

          // Clone the template table
          HTMLTableElement resultTable =
              (HTMLTableElement) resultTableTemplate.cloneNode(true);

          // ======== Create the header row ==========
          int numColumns = answer.getVariables().length;

          // Get the header row and make it wide enough
          HTMLTableRowElement headerRow =
              (HTMLTableRowElement) resultTable.getRows().item(0);

          // Make the row long enough
          HtmlTableBuilder.extendRow(headerRow, numColumns - 1);

          HTMLCollection headerCellCollection = headerRow.getCells();

          // Add the header data
          for (int column = 0; column < numColumns; column++) {

            String headerText = answer.getVariables()[column].getName();

            // Get the cell for the column
            HTMLTableCellElement headerCell =
                (HTMLTableCellElement) headerCellCollection.item(column);

            Text headerTextNode = DOMUtil.findFirstText(headerCell);
            headerTextNode.setData(headerText);
          }

          // ======== Create the result rows ==========
          // Get the template result row (row after header) and make it
          // wide enough
          HTMLTableRowElement row =
              (HTMLTableRowElement) resultTable.getRows().item(1);

          HtmlTableBuilder.extendRow(row, numColumns - 1);

          int rowCount = 0;

          // Add any solutions
          do {

            rowCount++;

            // Get the cells
            HTMLCollection cellCollection = row.getCells();

            // Add data to the row
            for (int column = 0; column < numColumns; column++) {

              Object data = answer.getObject(column);

              if (log.isDebugEnabled()) {

                log.debug("Adding object " + data + " of type " +
                    data.getClass() + ", column " + column);
              }

              // Get the cell for the column
              HTMLTableCellElement cell =
                  (HTMLTableCellElement) cellCollection.item(column);

              HTMLElement text =
                  (HTMLElement) cell.getElementsByTagName("SPAN").item(0);

              HTMLAnchorElement link =
                  (HTMLAnchorElement) cell.getElementsByTagName("A").item(0);

              // Remove any subtable in the template row
              NodeList tableList = cell.getElementsByTagName("TABLE");

              if (tableList.getLength() != 0) {

                org.w3c.dom.Node oldTable = tableList.item(0);
                oldTable.getParentNode().removeChild(oldTable);
              }

              // Sub query answer
              if (data instanceof Answer) {

                Answer subAnswer = (Answer) data;
                HTMLTableElement subResultTable =
                    createResultTable(subAnswer, ViewerScreen.RESULTS_PER_PAGE,
                    resultTableTemplate, modelURI);

                // Make the link invisible
                link.setAttribute(BComponent.VISIBILITY_MARKER, "false");

                if (subResultTable != null) {

                  // Make the text invisible
                  text.setAttribute(BComponent.VISIBILITY_MARKER, "false");
                  cell.appendChild(subResultTable);
                }
                else {

                  // Make the text visible
                  text.removeAttribute(BComponent.VISIBILITY_MARKER);
                  ( (Text) text.getFirstChild()).setData("No results found");
                }
                if (subAnswer != null) {
                  subAnswer.close();
                }
              }

              // RDF
              else if (data instanceof org.jrdf.graph.Node) {

                org.jrdf.graph.Node rdfNode = (org.jrdf.graph.Node) data;

                // If the node is null, add non-breaking space
                if (rdfNode == null) {

                  // Non-breaking space
                  DOMUtil.setTextInNode(text, "\u00A0", false);

                  // Make the text visible
                  text.removeAttribute(BComponent.VISIBILITY_MARKER);

                  // Make the link invisible
                  link.setAttribute(BComponent.VISIBILITY_MARKER, "false");
                }

                // Add the literal
                else if (rdfNode instanceof Literal) {

                  Literal literalNode = (Literal) rdfNode;
                  String displayText = literalNode.getEscapedForm();
                  String lexicalValue = literalNode.getLexicalForm().replaceAll("'", "\\\\'");
                  String linkText = "'" + lexicalValue + "'" +
                      appendType(literalNode.getDatatypeURI(),
                      literalNode.getLanguage());
                  DOMUtil.setTextInNode(link, displayText, false);

                  String linkHREF =
                      "ExecuteQuery.event?" + WebUIKeys.MODEL_URI + "=" +
                      URLEncoder.encode(modelURI, "UTF-8") +
                      "&" + WebUIKeys.QUERY_LITERAL + "=" +
                      URLEncoder.encode(linkText, "UTF-8");
                  link.setHref(linkHREF);

                  // Make the text invisible
                  text.setAttribute(BComponent.VISIBILITY_MARKER, "false");

                  // Make the link visible
                  link.removeAttribute(BComponent.VISIBILITY_MARKER);
                }

                // Add the resource URI
                else if (rdfNode instanceof URIReference) {

                  String linkText = ( (URIReference) rdfNode).getURI().toString();
                  DOMUtil.setTextInNode(link, linkText, false);

                  String linkHREF =
                      "ExecuteQuery.event?" + WebUIKeys.MODEL_URI + "=" +
                      URLEncoder.encode(modelURI, "UTF-8") + "&" +
                      WebUIKeys.QUERY_RESOURCE + "=" +
                      URLEncoder.encode(linkText, "UTF-8");
                  link.setHref(linkHREF);

                  // Make the text invisible
                  text.setAttribute(BComponent.VISIBILITY_MARKER, "false");

                  // Make the link visible
                  link.removeAttribute(BComponent.VISIBILITY_MARKER);
                }
                else if (rdfNode instanceof BlankNode) {

                  String linkText = ((BlankNode) rdfNode).toString();
                  DOMUtil.setTextInNode(text, linkText, false);

                  // Make the text visible
                  text.removeAttribute(BComponent.VISIBILITY_MARKER);

                  // Make the link invisible
                  link.setAttribute(BComponent.VISIBILITY_MARKER, "false");
                }
                else {

                  throw new Exception("Unknown RDFNode type: " +
                      rdfNode.getClass());
                }
              }
              else {

                // check for unbound value caused by OR
                if ( data == null ) {

                  DOMUtil.setTextInNode(text, "unbound", false);

                  // Make the text visible
                  text.removeAttribute(BComponent.VISIBILITY_MARKER);

                  // Make the link invisible
                  link.setAttribute(BComponent.VISIBILITY_MARKER, "false");
                } else {

                  throw new Exception("Unknown answer object type: " +
                                      data.getClass());
                }
              }
            }

            // Clone the node and add it
            HTMLTableRowElement tmpRow =
                (HTMLTableRowElement) row.cloneNode(true);
            HtmlTableBuilder.appendRow(resultTable, tmpRow);
          }
          while (answer.next() && rowCount < RESULTS_PER_PAGE);

          // Remove the template row
          resultTable.removeChild(row);

          return resultTable;
        }
        catch (Exception e) {

          throw new RuntimeException(
              "Unexpected error occurred while building the" + " result table",
              e);
        }
      }

      // No results
      else {

        return null;
      }
    }

    private String appendType(URI uri, String language) {
      String appendString = "";

      if (uri != null) {
        appendString = "^^<" + uri + ">";
      }
      else if (!language.equals("")) {
        appendString = "@" + language;
      }

      return appendString;
    }

    /**
     * Get a query string to display a resource.
     *
     * @param model The name of the model.
     * @param node The name of the node.
     * @return The query string.
     */
    private String buildResourceQuery(String model, String node) {

      StringBuffer urlString = new StringBuffer();
      urlString.append("select $Predicate $Object from <" + model +
          "> where <" + node + "> $Predicate $Object;");
      urlString.append("select $Subject $Predicate  from <" + model +
          "> where $Subject $Predicate <" + node + ">;");
      urlString.append("select $Subject $Object from <" + model +
          "> where $Subject <" + node + "> $Object;");

      return urlString.toString();
    }

    /**
     * Get a query string to display a literal.
     *
     * @param model The name of the model.
     * @param node The value of the literal.
     * @return The query string.
     */
    private String buildLiteralQuery(String model, String node) {

      String urlString =
          "select $Subject $Predicate  from <" + model +
          "> where $Subject $Predicate " + node + ";";

      return urlString;
    }
  }

  //------------------------------------------------------------
  //                View Event Handlers
  //------------------------------------------------------------

  /**
   * This is where we handle any RenderSelectModelScreen event and actually
   * generate the view. We repopulate the form if there is server information in
   * the session.
   */
  class RenderViewerScreenHandler
      extends DefaultViewHandler {

    /**
     * Handle the view event.
     *
     * @param root The root component which will get rendered as a result of
     *      this request.
     * @return The document to be rendered.
     * @throws InterruptDispatchException EXCEPTION TO DO
     */
    public Document handleViewEvent(BComponent root)
        throws InterruptDispatchException {

      return handleViewEvent(root,
          (ViewEventContext) getViewContext().getEventContext());
    }

    /**
     * Handle the view event.
     *
     * @param root The root component which will get rendered as a result of
     *      this request.
     * @param vc The ViewEventContext object describes what features the client
     *      view is capable of supporting.
     * @return The document to be rendered.
     * @throws InterruptDispatchException EXCEPTION TO DO
     */
    public Document handleViewEvent(BComponent root, ViewEventContext vc)
        throws InterruptDispatchException {

      Document dom = null;

      //Locale locale = vc.getViewCapabilities().getClientLocale();
      HttpServletRequest req = vc.getRequest();

      try {

        dom =
            DefaultDOMLoader.getGlobalInstance().getDOM(ViewerScreenHTML.class);

        //, locale);
      }
      catch (IOException e) {

        log.fatal("Fatal Error loading DOM template:", e);
      }

      BTemplate template = new BTemplate();
      root.addChild(template);

      org.w3c.dom.Node node = dom.getElementById("ViewerScreen");
      TemplateView view =
          new DefaultTemplateView(node, "id", new MapStateMap(properties));
      template.addView(view);

      template.addModel(new QueryDataModel());
      template.addModel(new QueryResultModel(req));

      return dom;
    }
  }

  //------------------------------------------------------------
  //                Components - TemplateModel
  //------------------------------------------------------------

  /**
   * SelectModelModel fills the query form from the session data.
   */
  class QueryDataModel
      extends AbstractTemplateModel {

    /**
     * The index of the selected model
     */
    private int selectedQueryIndex = 0;

    /**
     * Registers the model by name.
     */
    public String getName() {

      return "QueryData";
    }

    /**
     * Provides items by key.
     *
     * @param key The name of the item to get.
     * @return A value for the item. Can be a BComponent, String or DOM node.
     */
    public Object getItem(String key) {

      ViewContext vc = getViewContext();
      HttpSession session = SessionServices.getSession(vc, true);

      // The model URI
      if (key.equals(WebUIKeys.MODEL_URI)) {

        String model = (String) session.getAttribute(WebUIKeys.MODEL_URI);

        if (model == null) {

          model = modelURI;
        }

        return new BInput(BInput.TEXT, WebUIKeys.MODEL_URI, model);
      }

      // The actual query text
      if (key.equals(WebUIKeys.QUERY_TEXT)) {

        String query = (String) session.getAttribute(WebUIKeys.QUERY_TEXT);

        if (query == null) {

          query = "";
        }

        return new BText(query);
      }

      // The example query
      else if (key.equals(WebUIKeys.EXAMPLE_QUERY)) {

        BSelect querySelect = new BSelect(getExampleQueries(session));
        querySelect.setSelectedIndex(selectedQueryIndex);
        querySelect.setName(WebUIKeys.EXAMPLE_QUERY);

        return querySelect;
      }
      else if (key.equals("SubmitQuery")) {

        //return new BAction(new ExecuteQuery());
        BComponent component = new BComponent();
        component.setAttr("onClick",
            "document.QueryForm.action='ExecuteQuery.event'; validateAndSubmit()");

        return component;
      }
      else {

        return super.getItem(key);
      }
    }

    /**
     * Get a list of example queries.
     *
     * @param session PARAMETER TO DO
     * @return A list of example queries.
     */
    private ListModel getExampleQueries(HttpSession session) {

      DefaultListModel listModel = new DefaultListModel();

      String selectedQuery =
          (String) session.getAttribute(WebUIKeys.EXAMPLE_QUERY);

      if (selectedQuery == null) {

        selectedQuery = "";
      }

      // Default choice
      listModel.add(new DefaultItemMap("", "Select a query.."));

      // Iterate over the example queries.
      for (int i = 0; i < exampleQueryDisplay.size(); i++) {

        String displayText = (String) exampleQueryDisplay.get(i);
        String queryText = (String) exampleQueryText.get(i);

        listModel.add(new DefaultItemMap(queryText, displayText));

        if (queryText.equals(selectedQuery)) {

          // Add 1 because first entry is placeholder.
          selectedQueryIndex = i + 1;
        }
      }

      return listModel;
    }
  }

  /**
   * QueryResultModel populates the query result table.
   */
  class QueryResultModel
      extends AbstractTemplateModel {

    /**
     * The session for the user
     */
    private HttpSession session;

    /**
     * List of query results
     */
    private List resultList;

    /**
     * Public constructor.
     *
     * @param req The request this model represents data for.
     * @throws InterruptDispatchException EXCEPTION TO DO
     */
    public QueryResultModel(HttpServletRequest req)
        throws InterruptDispatchException {

      session = SessionServices.getSession(req, true);
      resultList = (List) session.getAttribute(WebUIKeys.RESULT);

      // Didn't come from controller, crete a new template screen
      if (session.getAttribute(WebUIKeys.CONTROLLER_FLAG) == null) {
        templateScreen = new ViewerScreenHTML(true);
      }
      // Reset for next time
      else {
        session.setAttribute(WebUIKeys.CONTROLLER_FLAG, null);
      }

      if (resultList == null) {
        resultList = new ArrayList();
      }
    }

    /**
     * Registers the model by name.
     *
     * @return The name of the model.
     */
    public String getName() {

      return "QueryResult";
    }

    /**
     * Provides items by key.
     *
     * @param key The name of the item to get.
     * @return A value for the item. Can be a String or DOM node.
     */
    public Object getItem(String key) {

      // Contains all result related nodes
      if (key.equals(WebUIKeys.RESULT_ROW)) {

        BComponent comp = new BComponent();

        if (resultList.size() <= 0) {

          comp.setVisible(false, true);
          return comp;
        }
        else {

          org.w3c.dom.Node cell = getResultCell();

          return viewContext.getElementFactory().getDocument().importNode(cell,
              true);
        }
      }

      // Unknown
      else {

        return super.getItem(key);
      }
    }

    /**
     * Build the result cell.
     *
     * @return The result cell.
     */
    private org.w3c.dom.Node getResultCell() {

      //The query executed
      String queryExecuted;

      // The message returned from the query being processed
      String resultMessage;

      // The result table for the result being processed
      HTMLTableElement resultTable;

      // The actual queries the results are for
      String[] queryTextArray = null;

      // Time formatter
      DecimalFormat format = new DecimalFormat();
      format.setMaximumFractionDigits(3);
      format.setMinimumFractionDigits(3);

      // Get the nodes we need
      HTMLTableCellElement cell = templateScreen.getElementResultCell();

      HTMLElement resultStatsTemplate = templateScreen.getElementResultStats();

      HTMLTableRowElement resultQueryRowTemplate =
          templateScreen.getElementResultQueryRow();
      HTMLElement queryExecutedTemplate =
          templateScreen.getElementQueryExecuted();

      HTMLTableRowElement resultMessageRowTemplate =
          templateScreen.getElementResultMessageRow();
      HTMLElement resultMessageTemplate =
          templateScreen.getElementResultMessage();

      HTMLTableRowElement resultSrcollRowTemplate =
          templateScreen.getElementResultScrollRow();
      HTMLAnchorElement resultForwardLink =
          templateScreen.getElementResultForwardLink();

      HTMLTableRowElement resultTableRowTemplate =
          templateScreen.getElementResultTableRow();
      HTMLTableCellElement resultTableCellTemplate =
          templateScreen.getElementResultTableCell();
      HTMLTableElement resultTableTemplate =
          templateScreen.getElementResultTable();

      // Break up the queries
      String queries = (String) session.getAttribute(WebUIKeys.QUERY_EXECUTED);

      if (queries != null) {

        queryTextArray = ItqlInterpreterBean.splitQuery(queries);
      }

      if (log.isDebugEnabled()) {
        log.debug("There are " + resultList.size() + " results to render");
      }

      // Populate the result rows
      for (int resultIndex = 0; resultIndex < resultList.size();
          resultIndex++) {

        // Query text
        queryExecuted = queryTextArray[resultIndex];
        DOMUtil.setTextInNode(queryExecutedTemplate, queryExecuted, false);

        QueryResult queryResult = (QueryResult) resultList.get(resultIndex);
        Object resultObject = queryResult.actualResult;

        // Got a message
        if (resultObject instanceof String) {

          resultMessage = (String) resultObject;

          if (log.isDebugEnabled()) {

            log.debug("Iteration " + resultIndex + " - got result message: " +
                resultMessage);
          }

          DOMUtil.setTextInNode(resultMessageTemplate, resultMessage, false);

          // Ensure the right rows are visible
          resultMessageRowTemplate.removeAttribute(BComponent.VISIBILITY_MARKER);
          resultTableRowTemplate.setAttribute(BComponent.VISIBILITY_MARKER,
              "false");
          resultSrcollRowTemplate.setAttribute(BComponent.VISIBILITY_MARKER,
              "false");
        }

        // Got a table
        else {

          resultTable = (HTMLTableElement) resultObject;

          if (log.isDebugEnabled()) {

            log.debug("Iteration " + resultIndex + " - got result table");
          }

          // Remove last table populated
          resultTableCellTemplate.removeChild(resultTableCellTemplate.
              getFirstChild());

          resultTable = (HTMLTableElement) templateScreen.getDocument().importNode(resultTable, true);
          resultTableCellTemplate.appendChild(resultTable);

          // Set scroll href
          if (resultTable.getRows().getLength() >= RESULTS_PER_PAGE) {
            String scrollHREF = null;

            try {
              scrollHREF =
                  "ExecuteQuery.event?" +
                  URLEncoder.encode(WebUIKeys.RESULT_INDEX + "=" + resultIndex,
                  "UTF-8");
            }
            catch (UnsupportedEncodingException ex) {/*UTF-8 is OK */}
            resultForwardLink.setHref(scrollHREF);

            resultSrcollRowTemplate.removeAttribute(BComponent.VISIBILITY_MARKER);
          }
          // Hide scroll - showing all rows
          else {
            resultSrcollRowTemplate.setAttribute(BComponent.VISIBILITY_MARKER,
                "false");
          }

          // Ensure the right rows are visible
          resultTableRowTemplate.removeAttribute(BComponent.VISIBILITY_MARKER);
          resultMessageRowTemplate.setAttribute(BComponent.VISIBILITY_MARKER,
              "false");
        }

        // Clone the nodes and insert them
        HTMLElement resultQueryRowClone =
            (HTMLElement) resultQueryRowTemplate.cloneNode(true);
        HTMLElement resultMessageRowClone =
            (HTMLElement) resultMessageRowTemplate.cloneNode(true);
        HTMLElement resultScrollRowClone =
            (HTMLElement) resultSrcollRowTemplate.cloneNode(true);
        HTMLElement resultTableRowClone =
            (HTMLElement) resultTableRowTemplate.cloneNode(true);

        org.w3c.dom.Node parentTable = resultQueryRowTemplate.getParentNode();

        parentTable.insertBefore(resultQueryRowClone, resultQueryRowTemplate);
        parentTable.insertBefore(resultMessageRowClone, resultQueryRowTemplate);
        parentTable.insertBefore(resultTableRowClone, resultQueryRowTemplate);
        parentTable.insertBefore(resultScrollRowClone, resultQueryRowTemplate);
      }

      // Remove the template nodes
      org.w3c.dom.Node parentTable = resultQueryRowTemplate.getParentNode();
      parentTable.removeChild(resultQueryRowTemplate);
      parentTable.removeChild(resultMessageRowTemplate);
      parentTable.removeChild(resultSrcollRowTemplate);
      parentTable.removeChild(resultTableRowTemplate);

      // Set the stats
      String serverTime = (String) session.getAttribute(WebUIKeys.RESULT_STATS);

      if ( (serverTime != null) &&
          (queryTextArray != null) &&
          (queryTextArray.length > 0)) {

        String stats;

        if (queryTextArray.length == 1) {

          stats =
              "(1 query, " + serverTime + " seconds)";
        }
        else {

          stats =
              "(" + queryTextArray.length + " queries, " + serverTime +
              " seconds)";
        }

        DOMUtil.setTextInNode(resultStatsTemplate, stats, false);
      }
      session.removeAttribute(WebUIKeys.RESULT_STATS);

      return cell;
    }
  }

  /**
   * A class to hold query results.
   */
  private class QueryResult {

    /** The answer from the DB if there was one */
    public Answer answer;

    /** Result for display, String or HTML Table*/
    public Object actualResult;
  }
}
