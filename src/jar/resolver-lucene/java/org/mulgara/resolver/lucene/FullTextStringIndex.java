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

package org.mulgara.resolver.lucene;

// Java 2 standard packages
import java.io.*;
import java.util.*;

// Log4J
import org.apache.log4j.*;

// Third party packages
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;

// Lucene text indexer
import org.apache.lucene.queryParser.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.*;

import org.mulgara.util.TempDir;


/**
 * The utility class which provides an interface of adding, finding and removing
 * statements and documents for Lucene.
 *
 * @created 2002-03-15
 *
 * @author <a href="http://staff.pisoftware.com/tate">Tate Jones</a>
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:47 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class FullTextStringIndex {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(FullTextStringIndex.class.getName());

  /**
   * The field name for the actual literal
   */
  public final static String ID_KEY = "id";

  /**
   * The field name for the stemmed literal
   */
  public final static String LITERAL_KEY = "stemmedliteral";

  /**
   * The field name for the actual subject
   */
  public final static String SUBJECT_KEY = "subject";

  /**
   * The field name for the actual predicate
   */
  public final static String PREDICATE_KEY = "predicate";

  /**
   * The field name for the reverse literal
   */
  public final static String REVERSE_LITERAL_KEY = "reverseliteral";

  /**
   * Disable reverse literal lookup
   */
  private static boolean enableReverseTextIndex;

  //
  // Constants
  //

  /**
   * The Directory for Lucene.
   */
  private Directory luceneIndexDirectory;

  /**
   * The name of the directory
   */
  private String indexDirectoryName;

  /**
   * The index writer
   */
  private IndexWriter indexer;

  /**
   * The index searcher
   */
  private IndexSearcher indexSearcher;

  /**
   * The index to perform deletions
   */
  private IndexReader indexDelete;

  /**
   *  Analyzer used for writing and reading
   */
  private Analyzer analyzer = getAnalyzer();

  /**
   * Locking object to stop multiple threads performing inserts, deletes and
   * optimizations are the same instance.
   */
  private IndexLock indexLock = new IndexLock();

  private String name;

  /**
   * Constructor for the FullTextStringIndex object.  Uses the system
   * property "mulgara.textindex.reverse.enabled" to set the desired value or
   * will default to "false" if not set.
   *
   * @param directory the directory to put the index files.
   * @throws FullTextStringIndexException Failure to initialize write index
   */
  public FullTextStringIndex(String directory, String newName)
      throws FullTextStringIndexException {

    name = newName;
    enableReverseTextIndex = System.getProperty(
        "mulgara.textindex.reverse.enabled", "false").equalsIgnoreCase("true");
    init(directory);
  }

  /**
   * Constructor for the FullTextStringIndex object
   *
   * @param directory the directory to put the index files.
   * @param newEnableReverseTextIndex true if you can begin Lucene queries with
   *      wildcards.
   * @throws FullTextStringIndexException Failure to initialize write index
   */
  public FullTextStringIndex(String directory, String newName,
      boolean newEnableReverseTextIndex) throws FullTextStringIndexException {

    name = newName;
    enableReverseTextIndex = newEnableReverseTextIndex;
    init(directory);
  }

  /**
   * Initialize the store.
   *
   * @param directory the directory to put the index files.
   * @throws FullTextStringIndexException Failure to initialize write index
   */
  private void init(String directory) throws FullTextStringIndexException {

    synchronized (indexLock) {
      try {
        Lock lock = FSDirectory.getDirectory(
            TempDir.getTempDir().getPath(), false
        ).makeLock(name);

        synchronized(lock) {
          lock.obtain();

          // create/open the indexes for reading and writing.
          initialize(directory);

          //set the status to not modified
          indexLock.setStatus(indexLock.NOT_MODIFIED);

          lock.release();
        }
      }
      catch (IOException ioe) {

        throw new FullTextStringIndexException(ioe);
      }
    }
  }

  /**
   * Get an instance of the analyzer used on text to produce the index.
   *
   * @return The analyzer used.
   */
  public static Analyzer getAnalyzer() {

    return new StandardAnalyzer();
  }

  /**
   * Gets the static
   * @param literal String
   * @return boolean
   */

  /**
   * Determine if the literal search string contains a leading wildcard.
   *
   * @param literal PARAMETER TO DO
   * @return The LeadingWildcard value
   */
  private static boolean isLeadingWildcard(String literal) {

    return (literal.startsWith("?") || literal.startsWith("*") ||
        (literal.indexOf(" *") >= 0) || (literal.indexOf(" ?") >= 0) ||
        (literal.indexOf("-*") >= 0) || (literal.indexOf("-?") >= 0) ||
        (literal.indexOf("+*") >= 0) || (literal.indexOf("+?") >= 0));
  }

  /**
   * Create a key to uniquely identify a triple Used for performing deletions.
   * TODO : hashcode is not the most appropriate technique. In future change to
   * MD5 sum.
   *
   * @param subject PARAMETER TO DO
   * @param predicate PARAMETER TO DO
   * @param literal PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  private static String createKey(String subject, String predicate,
      String literal) {

    return String.valueOf(subject.hashCode()) +
        String.valueOf(predicate.hashCode()) + String.valueOf(literal.hashCode());
  }

  /**
   * Reverse the literal search string to ensure the + and - contraints are
   * prefixed.
   *
   * @param literal PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  private static String reverseLiteralSearch(String literal) {

    String newReversedString = null;

    // does it contain any +'s or -'s in the search request?
    if ( (literal.indexOf("+") >= 0) || (literal.indexOf("-") >= 0)) {

      StringBuffer searchReversed = new StringBuffer();
      String[] tokens = literal.split(" ");

      for (int i = tokens.length - 1; i >= 0; i--) {

        StringBuffer reversedStringBuff = new StringBuffer(tokens[i]).reverse();

        char lastChar =
            reversedStringBuff.charAt(reversedStringBuff.length() - 1);

        // move the + || - to the start of the reversed string
        if ( (lastChar == '+') || (lastChar == '-')) {

          reversedStringBuff.deleteCharAt(reversedStringBuff.length() - 1)
              .insert(0, lastChar);
        }

        searchReversed.append(reversedStringBuff);

        if (i != 0) {

          searchReversed.append(" ");
        }
      }

      newReversedString = searchReversed.toString();
    }
    else {

      // perform a simple reverse
      newReversedString = (new StringBuffer(literal).reverse()).toString();
    }

    if (logger.isDebugEnabled()) {

      logger.debug("Reversed literal search from : " + literal + " to " +
          newReversedString);
    }

    return newReversedString;
  }

  /**
   * Add a subject, predicate and literal into the fulltext string pool. {@link
   * StandardAnalyzer} sets the filters used to on the literal field index.
   *
   * @param subject the subject to be added
   * @param predicate the predicate to be added
   * @param literal literal to be analyzed for fulltext searching
   * @return boolean Return true if successful
   * @throws FullTextStringIndexException Failure to add string due to an
   *      IOException
   */
  public boolean add(String subject, String predicate,
      String literal) throws FullTextStringIndexException {

    if ( literal == null   ||
         subject == null   || subject.length() == 0 ||
         predicate == null || predicate.length() == 0) {

      throw new FullTextStringIndexException(
          "Subject, predicate or literal has " + "not been supplied a value");
    }

    boolean added = false;

    // Warn for an empty literal
    if (literal.trim().length() == 0) {
      logger.warn("Ignoring empty literal");
    }
    else {

      // debug logging
      if (logger.isDebugEnabled()) {

        logger.debug("Adding subject <" + subject + "> predicate <" + predicate +
            "> " + " literal <'" + literal + "'> to fulltext string index");
      }

      Document indexDocument = new Document();

      // Add the literal value to the predicate field and tokenize it for
      // fulltext searching
      indexDocument.add(new Field(LITERAL_KEY, literal, true, true, true));

      // Add the literal value to the predicate field and tokenize it for
      // fulltext searching in reverse order
      if (enableReverseTextIndex) {

        indexDocument.add(new Field(REVERSE_LITERAL_KEY,
            (new StringBuffer(literal).reverse()).toString(), true, true, true));
      }

      // Add the actual literal, do not tokenize it. Required for exact
      // matching. ie. removal
      indexDocument.add(new Field(ID_KEY,
          this.createKey(subject, predicate, literal), true, true, false));

      // Add the predicate, do not tokenize it, required for exact matching
      indexDocument.add(new Field(PREDICATE_KEY, predicate, true, true, false));

      // Add the subject, do not tokenize it, required for exact matching
      indexDocument.add(new Field(SUBJECT_KEY, subject, true, true, false));

      try {

        //lock any deletes, adds and optimize from the index
        synchronized (indexLock) {

          // add to writer
          indexer.addDocument(indexDocument);

          // Update the status of the index
          indexLock.setStatus(indexLock.MODIFIED);
        }

        added = true;
      }
      catch (IOException ex) {

        logger.error("Unable to add fulltext string subject <" + subject +
            "> predicate <" + predicate + "> literal <'" + literal +
            "'> to fulltext string index", ex);
        throw new FullTextStringIndexException(
            "Unable to add fulltext string subject <" + subject + "> predicate <" +
            predicate + "> literal <'" + literal + "'> to fulltext string index",
            ex);
      }

    } // Warn empty literals

    return added;
  }

  /**
   * Add a subject, predicate and literal into the fulltext string pool. {@link
   * StandardAnalyzer} sets the filters used to on the literal field index.
   *
   * @param subject the subject to be added
   * @param predicate the predicate to be added
   * @param resource resource to be analyzed for fulltext searching
   * @param reader stream containing text of the resource
   * @return <code>true</code> if successful
   * @throws FullTextStringIndexException Failure to add string due to an
   *      IOException
   */
  public boolean add(String subject, String predicate, String resource,
      Reader reader) throws FullTextStringIndexException {

    // Validate "subject" parameter
    if ( (subject == null) || (subject.length() == 0)) {

      throw new FullTextStringIndexException("No \"subject\" parameter");
    }

    // Validate "predicate" parameter
    if ( (predicate == null) || (predicate.length() == 0)) {

      throw new FullTextStringIndexException("No \"predicate\" parameter");
    }

    // Validate "resource" parameter
    if ( (resource == null) || (resource.length() == 0)) {

      throw new FullTextStringIndexException("No \"resource\" parameter");
    }

    // Validate "reader" parameter
    if (reader == null) {

      throw new FullTextStringIndexException("Null \"reader\" parameter");
    }

    boolean added = false;

    // debug logging
    if (logger.isDebugEnabled()) {

      logger.debug("Adding subject <" + subject + "> predicate <" + predicate +
          "> resource <" + resource + "> to fulltext string index");
    }

    Document indexDocument = new Document();

    // Add the resource content to the predicate field and tokenize it for
    // fulltext searching
    indexDocument.add(Field.Text(LITERAL_KEY, reader));

    // Add the resource label, do not tokenize it. Required for exact
    // matching. ie. removal
    indexDocument.add(new Field(ID_KEY,
        this.createKey(subject, predicate, resource), true, true, false));

    // Add the predicate, do not tokenize it, required for exact matching
    indexDocument.add(new Field(PREDICATE_KEY, predicate, true, true, false));

    // Add the subject, do not tokenize it, required for exact matching
    indexDocument.add(new Field(SUBJECT_KEY, subject, true, true, false));

    try {

      //lock any deletes, adds and optimize from the index
      synchronized (indexLock) {

        // add to writer
        indexer.addDocument(indexDocument);

        // Update the status of the index
        indexLock.setStatus(indexLock.MODIFIED);
      }

      added = true;
    }
    catch (IOException ex) {

      logger.error("Unable to add fulltext string subject <" + subject +
          "> predicate <" + predicate + "> resource <" + resource +
          "> to fulltext string index", ex);
      throw new FullTextStringIndexException(
          "Unable to add fulltext string subject <" + subject + "> predicate <" +
          predicate + "> resource <" + resource + "> to fulltext string index",
          ex);
    }

    return added;
  }

  /**
   * Add a document into the fulltext string pool. The constants {@link
   * #SUBJECT_KEY}, {@link #PREDICATE_KEY}, {@link #LITERAL_KEY} should be used
       * in the query to reference the relevant index fields if the index is to read
   * by queries.
   *
   * @param indexDocument The document to be indexed.
   * @return true if successful
   * @throws FullTextStringIndexException Failure to add string due to an
   *      IOException
   */
  public boolean add(Document indexDocument) throws
      FullTextStringIndexException {

    if (indexDocument == null) {

      throw new FullTextStringIndexException(
          "The document to be indexed was null.");
    }

    boolean added = false;

    // debug logging
    if (logger.isDebugEnabled()) {

      logger.debug("Adding document " + indexDocument +
          " to fulltext string index");
    }

    try {

      //lock any deletes, adds and optimize from the index
      synchronized (indexLock) {

        // add to writer
        indexer.addDocument(indexDocument);

        // Update the status of the index
        indexLock.setStatus(indexLock.MODIFIED);

        added = true;
      }
    }
    catch (IOException ex) {

      logger.error("Unable to add " + indexDocument +
          " to fulltext string index", ex);
      throw new FullTextStringIndexException("Unable to add " + indexDocument +
          " to fulltext string index", ex);
    }

    return added;
  }

  /**
   * Remove all index files from the current initialised directory WARNING : All
   * files are removed in the specified directory.
   *
   * @return return true if successful at removing all index files
   * @throws FullTextStringIndexException Exception occurs when attempting to
   *      close the indexes
   */
  public boolean removeAll() throws FullTextStringIndexException {

    // debug logging
    if (logger.isDebugEnabled()) {
      logger.debug("Removing all indexes from " + luceneIndexDirectory.toString());
    }

    boolean deleted = false;

    //Delete the directory if it exists
    if (luceneIndexDirectory != null) {

      //lock any deletes, adds and optimize from the index
      synchronized (indexLock) {

        //Close the reading and writing indexes
        this.close();

        try {

          Lock lock = FSDirectory.getDirectory(
              TempDir.getTempDir().getPath(), false
          ).makeLock(name);

          synchronized(lock) {

            lock.obtain();

            //Remove all files from the directory
            String[] files = luceneIndexDirectory.list();

            for (int i = 0; i < files.length; i++) {

              luceneIndexDirectory.deleteFile(files[i]);
            }

            //Remove the directory
            deleted = new File(indexDirectoryName).delete();

            //set the status to not modified
            indexLock.setStatus(indexLock.NOT_MODIFIED);

            lock.release();
          }
        }
        catch (IOException ioe) {

          throw new FullTextStringIndexException(ioe);
        }
      }
    }

    return deleted;
  }

  /**
   * Remove the extact string from the fulltext string pool
   *
   * @param subject subject must be supplied
   * @param predicate predicate must be supplied
   * @param literal literal must be supplied
   * @return True is the string was successfully removed
   * @throws FullTextStringIndexException An IOException occurs on index
   *      modification
   */
  public boolean remove(String subject, String predicate,
      String literal) throws FullTextStringIndexException {

    boolean removed = false;

    if ( (literal == null) ||
        (literal.length() == 0) ||
        (subject == null) ||
        (subject.length() == 0) ||
        (predicate == null) ||
        (predicate.length() == 0)) {

      throw new FullTextStringIndexException(
          "Subject, predicate or literal has " + "not been supplied a value");
    }

    //Create the composite key for searching
    String key = this.createKey(subject, predicate, literal);

    try {

      Term term = new Term(ID_KEY, key);

      //lock any deletes, adds and optimize from the index
      synchronized (indexLock) {

        //check the status of the index
        if (indexLock.getStatus() == indexLock.MODIFIED) {

          // re-open the read index - the read index performs deletions
          // 25-2-03 - Leave the current index open.
          // Closing it will currupt any existing hits that
          // are currently open.
          // openReadIndex();
        }

        int deleted = indexDelete.delete(term);

        //set the index status to modified
        indexLock.setStatus(indexLock.MODIFIED);

        removed = (deleted > 0);
      }

      if (logger.isDebugEnabled()) {

        if (removed) {

          logger.debug("Removed key '" + key + "' from fulltext string pool");
        }
        else {

          logger.debug("Unable to removed string '" + key +
              "' from fulltext string pool");
        }
      }
    }
    catch (IOException ex) {

      logger.error("Unable to delete the string '" + key + "'", ex);
      throw new FullTextStringIndexException("Unable to delete the string '" +
          key + "'", ex);
    }

    return removed;
  }

  /**
   * Close the indexes on disk.
   *
   * @throws FullTextStringIndexException if there is an error whilst saving the
   *      index.
   */
  public void close() throws FullTextStringIndexException {

    try {

      if (indexer != null) {
        indexer.close();
        indexer = null;
      }

      if (indexSearcher != null) {
        indexSearcher.close();
        indexSearcher = null;
      }

      if (indexDelete != null) {
        indexDelete.close();
        indexDelete = null;
      }
    }
    catch (IOException ex) {

      logger.error("Unable to close fulltext string pool indexes", ex);
      throw new FullTextStringIndexException(
          "Unable to close fulltext string pool indexes",
          ex);
    }
  }

  /**
   * Optimize the index and then flush it to disk.
   *
       * @throws FullTextStringIndexException If there was a problem reading from or
   *      writing to the disk.
   */
  public void optimize() throws FullTextStringIndexException {

    // debug logging
    if (logger.isDebugEnabled()) {

      logger.debug("Optimizing fulltext string pool indexes to disk");
    }

    try {

      //lock any deletes, adds and optimize from the index
      synchronized (indexLock) {

        logger.info("Optimizing fulltext index at " + this.indexDirectoryName +
            " please wait...");

        //Optimize the indexes
        indexer.optimize();

        //set the index status to modified
        indexLock.setStatus(indexLock.MODIFIED);
      }
    }
    catch (IOException ex) {

      logger.error("Unable to optimize existing fulltext string pool index", ex);
      throw new FullTextStringIndexException(
          "Unable to optimize existing fulltext " + "string pool index",
          ex);
    }
  }

  /**
   * Find a string within the fulltext string pool. The search is based on the
   * {@link StandardAnalyzer} used to add the string. Subject and predicate can
   * be supplied {null}.
   *
   * @param subject subject maybe null
   * @param predicate predicate maybe null
   * @param literal literal to be searched via the analyzer. Must always be
   *      supplied
   * @return Object containing the hits
   * @throws FullTextStringIndexException IOException occurs on reading index
   */
  public Hits find(String subject, String predicate,
      String literal) throws FullTextStringIndexException {

    if ((literal == null) || (literal.length() == 0)) {

      throw new FullTextStringIndexException(
          "Literal has not been supplied a value");
    }

    Hits hits = null;
    BooleanQuery bQuery = new BooleanQuery();

    try {

      // debug logging
      if (logger.isDebugEnabled()) {
        logger.debug("Searching the fulltext string index pool with " +
            " subject :" + subject + " predicate :" + predicate + " literal :" +
            literal);
      }

      if ((subject != null) && (subject.length() > 0)) {
        TermQuery tSubject = new TermQuery(new Term(SUBJECT_KEY, subject));
        bQuery.add(tSubject, true, false);
      }

      if ((predicate != null) && (predicate.length() > 0)) {
        TermQuery tPredicate =
            new TermQuery(new Term(PREDICATE_KEY, predicate));
        bQuery.add(tPredicate, true, false);
      }

      Query qliteral = null;

      // Are we performing a reverse string lookup?
      if (enableReverseTextIndex && isLeadingWildcard(literal)) {
        literal = reverseLiteralSearch(literal);
        qliteral = QueryParser.parse(literal, REVERSE_LITERAL_KEY, analyzer);
      }
      else {
        qliteral = QueryParser.parse(literal, LITERAL_KEY, analyzer);
      }

      // submit the literal to the boolean query
      bQuery.add(qliteral, true, false);

      // debug logging
      if (logger.isDebugEnabled()) {

        if ( (literal.startsWith("*") || literal.startsWith("?")) &&
            enableReverseTextIndex) {

          logger.debug(
              "Searching the fulltext string index pool with parsed query as " +
              bQuery.toString(REVERSE_LITERAL_KEY));
        }
        else {

          logger.debug(
              "Searching the fulltext string index pool with parsed query as " +
              bQuery.toString(LITERAL_KEY));
        }
      }

      //wait for locks performing deletes, adds and optimizes
      synchronized (indexLock) {

        //check the status of the index
        if (indexLock.getStatus() == indexLock.MODIFIED) {

          // re-open the read index
          openReadIndex();

          //set the index status to not modified
          indexLock.setStatus(indexLock.NOT_MODIFIED);
        }
      }

      //Perform query
      hits = indexSearcher.search(bQuery);

      if (logger.isDebugEnabled()) {

        logger.debug("Got hits: " + hits.length());
      }
    }
    catch (IOException ex) {

      logger.error("Unable to read results for query '" +
          bQuery.toString(LITERAL_KEY) + "'", ex);
      throw new FullTextStringIndexException(
          "Unable to read results for query '" + bQuery.toString(LITERAL_KEY) +
          "'",
          ex);
    }
    catch (org.apache.lucene.queryParser.ParseException ex) {

      logger.error("Unable to parse query '" + bQuery.toString(LITERAL_KEY) +
          "'", ex);
      throw new FullTextStringIndexException("Unable to parse query '" +
          bQuery.toString(LITERAL_KEY) + "'", ex);
    }

    return hits;
  }

  /**
   * Execute a query against the string pool. The constants {@link
   * #SUBJECT_KEY}, {@link #PREDICATE_KEY}, {@link #LITERAL_KEY} should be used
       * in the query to reference the relevant index filds if the index was created
   * by queries. Use the method {@link #getAnalyzer()} to get the analyzer
   * used by this class.
   *
   * @param query The query to execute.
   * @return RETURNED VALUE TO DO
   * @throws IOException occurs on reading index.
   * @throws FullTextStringIndexException EXCEPTION TO DO
   */
  public Hits find(Query query) throws FullTextStringIndexException {

    if (query == null) {

      throw new FullTextStringIndexException("The query may not be null.");
    }

    Hits hits = null;

    try {

      // debug logging
      if (logger.isDebugEnabled()) {

        logger.debug("Searching the fulltext string index pool with query " +
            query.toString(LITERAL_KEY));
      }

      //wait for locks performing deletes, adds and optimizes
      synchronized (indexLock) {

        //check the status of the index
        if (indexLock.getStatus() == indexLock.MODIFIED) {

          // re-open the read index
          openReadIndex();

          //set the index status to not modified
          indexLock.setStatus(indexLock.NOT_MODIFIED);
        }
      }

      //Perform query
      hits = indexSearcher.search(query);
    }
    catch (IOException ex) {

      logger.error("Unable to read results for query '" +
          query.toString(LITERAL_KEY) + "'", ex);
      throw new FullTextStringIndexException(
          "Unable to read results for query '" + query.toString(LITERAL_KEY) +
          "'",
          ex);
    }

    return hits;
  }

  /**
   * Verify and initialize the indexes for reading. Will automatically create
   * indexes if they do not exist.
   *
   * @param directory Directory of the index to be initialized
   * @throws FullTextStringIndexException IOException occurs while trying to
   *      locate or create the indexes
   */
  private void initialize(String directory) throws FullTextStringIndexException {

    // debug logging
    if (logger.isDebugEnabled()) {
      logger.debug("Initialization of FullTextStringIndex to directory to " +
          directory);
    }

    File indexDirectory;

    try {
      indexDirectoryName = directory;
      indexDirectory = new File(directory);
    }
    catch (Exception ex) {
      logger.error("Exception when initializing fulltext string index " +
          "directory", ex);
      throw new FullTextStringIndexException("Exception when initializing " +
          "fulltext " + "string index directory", ex);
    }

    // Ensure index is flushed to disk before reading config.
    this.close();

    // does the directory exist?
    if (!indexDirectory.exists()) {

      // no, make it
      indexDirectory.mkdirs();
    }

    // ensure the index directory is a directory
    if (!indexDirectory.isDirectory()) {
      indexDirectory = null;
      logger.fatal("The fulltext string index directory '" + directory +
          "' does not exist!");
      throw new FullTextStringIndexException(
          "The fulltext string index index directory '" + indexDirectory +
          "' does not exist!");
    }

    // ensure the directory is writeable
    if (!indexDirectory.canWrite()) {
      indexDirectory = null;
      logger.fatal("The fulltext string index directory '" + directory +
          "' is not writeable!");
      throw new FullTextStringIndexException(
          "The fulltext string index directory '" + directory +
          "' is not writeable!");
    }

    // Create lucene directory.
    try {
      luceneIndexDirectory = FSDirectory.getDirectory(directory, false);
    }
    catch (IOException ioe) {
      throw new FullTextStringIndexException(ioe);
    }

    assert luceneIndexDirectory != null;

    // Open the index for writing
    try {
      openWriteIndex();
    }
    catch (LockFailedException lfe) {

      // If it fails once try and unlock the directory and try again.
      try {
        IndexReader.unlock(luceneIndexDirectory);
      }
      catch (IOException ioe) {
        throw new FullTextStringIndexException("Failed to unlock directory: " +
            luceneIndexDirectory, ioe);
      }

      // Try again - let it fail this time.
      openWriteIndex();
    }

    // Open the index for reading
    openReadIndex();

    // debug logging
    if (logger.isDebugEnabled()) {
      logger.debug("Fulltext string index initialized");
    }
  }

  /**
   * Open the index on disk for writing.
   *
   * @throws FullTextStringIndexException if there is an error whilst opening
   *      the index.
   */
  private void openWriteIndex() throws FullTextStringIndexException {

    // debug logging
    if (logger.isDebugEnabled()) {
      logger.debug("Opening index for IndexWriter and IndexReader " +
          "(deletions): " + luceneIndexDirectory);
    }

    // Create a new index.
    if (indexer == null) {

      // Try to use an existing index.
      try {
        indexer = new IndexWriter(luceneIndexDirectory, analyzer, false);
      }
      catch (FileNotFoundException fnfe) {

        // File not found means that it doesn't exist - create a new one.
        if (logger.isDebugEnabled()) {
          logger.error("Failed to open read only, opening with write " +
              "enabled: " + luceneIndexDirectory, fnfe);
        }

        // Try to create a new index.
        try {
          luceneIndexDirectory = FSDirectory.getDirectory(indexDirectoryName,
              true);
          indexer = new IndexWriter(luceneIndexDirectory, analyzer, true);
        }
        catch (IOException ioe) {

          if (ioe.getMessage().indexOf("couldn't delete") >= 0) {
            throw new LockFailedException(ioe);
          }

          logger.error("Unable to new existing fulltext string pool: " +
              luceneIndexDirectory, ioe);
          throw new FullTextStringIndexException("Unable to open " +
              "new fulltext string pool", ioe);
        }
      }
      catch (IOException ioe) {

        // If the IOE was because of the lock failing report it.
        if (ioe.getMessage().indexOf("Lock obtain") >= 0) {
          throw new LockFailedException(ioe);
        }

        throw new FullTextStringIndexException("Unable to create existing " +
            "fulltext string pool index", ioe);
      }
    }
    else {
      logger.error("Tried to create a new fulltext string index when " +
          "there already exists one");
      throw new FullTextStringIndexException(
          "Tried to create a new indexer when " + "there already exists one");
    }
  }

  /**
   * Open the index on disk for reading and deleting
   *
   * @throws FullTextStringIndexException if there is an error whilst opening
   *      the index.
   */
  private void openReadIndex() throws FullTextStringIndexException {

    // debug logging
    if (logger.isDebugEnabled()) {

      logger.debug("Opening index for IndexSearcher");
    }

    optimize();

    try {

      if (indexSearcher != null) {

        indexSearcher.close();
        indexSearcher = null;
      }

      if (indexDelete != null) {

        indexDelete.close();
        indexDelete = null;
      }

      // Try to use existing index.
      indexDelete = indexDelete.open(luceneIndexDirectory);
      indexSearcher = new IndexSearcher(indexDelete);
    }
    catch (IOException ex) {

      logger.error("Unable to open existing fulltext string pool " +
          "index for searching", ex);
      throw new FullTextStringIndexException(
          "Unable to open existing fulltext " + "string pool index for reading",
          ex);
    }
  }

  /**
   * Locking object to stop multiple threads performing inserts, deletes and
   * optimizations are the same instance.
   */
  private class IndexLock {

    final static int MODIFIED = 1;

    final static int NOT_MODIFIED = 0;

    private int status = NOT_MODIFIED;

    public void setStatus(int status) {

      this.status = status;
    }

    public int getStatus() {

      return this.status;
    }
  }
}
