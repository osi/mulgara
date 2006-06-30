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

import java.io.File;
import java.net.InetAddress;
import java.net.URI;

import org.kowari.server.Session;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import org.kowari.server.driver.SessionFactoryFinder;
import org.kowari.server.SessionFactory;

/**
 * A sample application showing how to use Kowari and Jena's reifier together.
 *
 * @created 2004-09-27
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:17 $
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
public class ReificationSupportExample {

  static Model getKowariModel() throws Exception {
    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    URI serverURI = new URI("rmi", hostname, "/server1", null);

    SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(
        serverURI, false);
    LocalJenaSession session = (LocalJenaSession) sessionFactory.
          newJenaSession();

      ModelMaker modelMaker = new ModelKowariMaker(new GraphKowariMaker(
        session, serverURI, ReificationStyle.Convenient));
    String modelName = "testbot";
    if (modelMaker.hasModel(modelName)) {
      return modelMaker.openModel(modelName);
    }
    else {
      Model model = modelMaker.createModel(modelName);
      if (model.size() == 0) {
        System.out.println("new model, inserting statement");
        Statement stmt = model.createStatement(RDFS.Literal,
            RDFS.comment, "literal");
        model.add(stmt);
        ReifiedStatement rs = stmt.createReifiedStatement();
        System.out.println("and a statement about its reification");
        model.add(rs, RDF.type, model
            .createResource("http://example.org/foo"));
        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
          System.out.println(stmts.nextStatement());
        }
        stmts.close();
      }
      return model;
    }
  }

  public static void main(String[] args) throws Exception {
    Model model = getKowariModel();

    System.out.println("initial size " + model.size());
    model.write(System.out);

    ReifiedStatement reified;
    Resource res;
    res = model.listSubjectsWithProperty(RDF.type,
        model.createResource("http://example.org/foo")).nextResource();
    if (res.canAs(ReifiedStatement.class)) {
      reified = (ReifiedStatement) res.as(ReifiedStatement.class);
      System.err.println("Got: " + reified);
    }
    System.out.println("end size " + model.size());
    System.out.println("finished");
    model.close();
  }
}
