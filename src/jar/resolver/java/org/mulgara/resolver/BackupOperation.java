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
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import org.jrdf.graph.URIReference;
import org.mulgara.content.rdfxml.writer.RDFXMLWriter;
import org.mulgara.query.Constraint;
import org.mulgara.query.ConstraintImpl;
import org.mulgara.query.LocalNode;
import org.mulgara.query.Variable;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.DatabaseMetadata;
import org.mulgara.resolver.spi.Resolution;
import org.mulgara.resolver.spi.Resolver;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Statements;
import org.mulgara.resolver.spi.SystemResolver;
import org.mulgara.resolver.spi.TuplesWrapperStatements;
import org.mulgara.store.statement.StatementStore;
import org.mulgara.store.stringpool.SPObject;
import org.mulgara.store.tuples.Tuples;

/**
 * An {@link Operation} that serializes the state of the database into a backup
 * file which can be read back by the complementary {@link RestoreOperation}.
 *
 * @created 2004-10-07
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.9 $
 * @modified $Date: 2005/02/22 08:16:06 $ by $Author: newmana $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class BackupOperation extends OutputOperation implements BackupConstants, Operation {

  private final URI serverURI;

  //
  // Constructor
  //

  /**
   * Create an {@link Operation} that backs up all the data on the specified
   * server to a URI or an output stream.
   *
   * The database is not changed by this method.
   * If an {@link OutputStream} is supplied then the destinationURI is ignored.
   *
   * @param outputStream  output stream to receive the contents, may be
   *   <code>null</code> if a <var>destinationURI</var> is specified
   * @param serverURI The URI of the server to backup, never <code>null</code>
   * @param destinationURI  URI of the file to backup into, may be
   *   <code>null</code> if an <var>outputStream</var> is specified
   */
  public BackupOperation(OutputStream outputStream, URI serverURI, URI destinationURI) {
    super(outputStream, destinationURI);
    this.serverURI = serverURI;
  }

  //
  // Methods implementing Operation
  //

  public void execute(OperationContext operationContext,
      SystemResolver systemResolver,
      DatabaseMetadata metadata) throws Exception {
    OutputStream os = outputStream;
    Writer writer = null;
    try {
      os = getOutputStream();

      // The existence of a fragment indicates that a model is to be backed
      // up otherwise the entire database is to be backed up.
      if (serverURI != null && serverURI.getFragment() != null) {
        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
        writer = osw;

        // Export as RDF/XML.
        backupModel(systemResolver, systemResolver, serverURI, osw);
      } else {
        writer = new BufferedWriter(new OutputStreamWriter(
            new GZIPOutputStream(os), "UTF-8"
        ));

        backupDatabase(systemResolver, metadata, writer);
      }
    } finally {
      // Clean up.
      if (writer != null) {
        // Close the writer if it exists.  This will also close the wrapped
        // OutputStream.
        writer.close();
      } else if (os != null) {
        // Close the os if it exists.
        os.close();
      }
    }
  }


  /**
   * Dumps the entire database to the specified Writer.
   *
   * @param stringPool StringPool
   * @param resolver Resolver
   * @param metadata DatabaseMetadata
   * @param writer Writer
   * @throws Exception
   */
  private void backupDatabase(SystemResolver systemResolver, DatabaseMetadata metadata, Writer writer)
      throws Exception {
    // Write the backup
    writer.write(BACKUP_FILE_HEADER + BACKUP_VERSION + '\n');
    writer.write(new Date().toString());
    writer.write('\n');

    // Dump the strings.
    writer.write("RDFNODES\n");

    Tuples t = systemResolver.findStringPoolType(null, null);
    assert t != null;
    try {
      t.beforeFirst();
      while (t.next()) {
        long localNode = t.getColumnValue(0);
        writer.write(Long.toString(localNode));
        writer.write(' ');

        SPObject spObject = systemResolver.findSPObject(localNode);
        writer.write(spObject.getEncodedString());
        writer.write('\n');
      }
    } finally {
      t.close();
    }

    // Dump the triples.
    Tuples tuples = systemResolver.resolve(new ConstraintImpl(
        StatementStore.VARIABLES[0],
        StatementStore.VARIABLES[1],
        StatementStore.VARIABLES[2],
        StatementStore.VARIABLES[3]));
    assert tuples != null;
    try {
      assert tuples.getVariables()[0] == StatementStore.VARIABLES[0];
      assert tuples.getVariables()[1] == StatementStore.VARIABLES[1];
      assert tuples.getVariables()[2] == StatementStore.VARIABLES[2];
      assert tuples.getVariables()[3] == StatementStore.VARIABLES[3];
      writer.write("TRIPLES\n");

      long preallocationModelNode = metadata.getPreallocationModelNode();
      for (tuples.beforeFirst(); tuples.next(); ) {
        // Suppress output of the preallocation model.
        long modelNode = tuples.getColumnValue(3);
        if (modelNode != preallocationModelNode) {
          writer.write(Long.toString(tuples.getColumnValue(0)));
          writer.write(' ');
          writer.write(Long.toString(tuples.getColumnValue(1)));
          writer.write(' ');
          writer.write(Long.toString(tuples.getColumnValue(2)));
          writer.write(' ');
          writer.write(Long.toString(modelNode));
          writer.write('\n');
        }
      }
    } finally {
      tuples.close();
    }

    writer.write("END\n");
  }


  /**
   * Obtains statements for the model and writes to an RDFXMLWriter.
   *
   * @param resolver Resolver
   * @param session ResolverSession
   * @param modelURI URI
   * @param writer OutputStreamWriter
   * @throws Exception
   */
  private void backupModel(
      Resolver resolver, ResolverSession session, URI modelURI,
      OutputStreamWriter writer
  ) throws Exception {
    // get the meta node for the model
    URIReference modelValue = new URIReferenceImpl(modelURI);
    LocalNode modelNode = new LocalNode(
        session.lookupPersistent(modelValue)
    );

    // create a constraint to get all statements
    Variable[] vars = new Variable[] {
        StatementStore.VARIABLES[0],
        StatementStore.VARIABLES[1],
        StatementStore.VARIABLES[2]
    };

    // get all statements for the model
    Constraint constraint = new ConstraintImpl(vars[0], vars[1], vars[2],
        modelNode);
    Resolution resolution = resolver.resolve(constraint);

    // convert to Statements Object
    Statements modelStatements = new TuplesWrapperStatements(
        resolution, vars[0], vars[1], vars[2]
    );

    // do the writing
    try {
      RDFXMLWriter rdfWriter = new RDFXMLWriter();
      rdfWriter.write(modelStatements, session, writer);
    } finally {
      modelStatements.close();
    }
  }

  /**
   * @return <code>false</code>
   */
  public boolean isWriteOperation() {
    return false;
  }

}
