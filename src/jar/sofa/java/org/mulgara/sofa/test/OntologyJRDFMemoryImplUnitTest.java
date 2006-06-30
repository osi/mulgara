package org.mulgara.sofa.test;

import net.java.dev.sofa.impl.OntoConnector;
import java.util.*;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;


import org.jrdf.graph.*;
import com.sun.rsasign.t;

import junit.framework.TestCase;
import net.java.dev.sofa.*;
import net.java.dev.sofa.impl.*;
import net.java.dev.sofa.model.*;
import net.java.dev.sofa.model.mem.OntologyMemoryModel;
import net.java.dev.sofa.serialize.daml.*;
import net.java.dev.sofa.serialize.rdfs.*;
import net.java.dev.sofa.serialize.owl.*;
import net.java.dev.sofa.serialize.visual.*;
import net.java.dev.sofa.vocabulary.SOFA;

import org.mulgara.jrdf.JRDFGraph;
import org.mulgara.sofa.*;

/**
 * TODO ONE LINE DESC <p>
 *
 * TODO MORE DETAILED DESC
 * </p>
 *
 * @created Sep 1, 2004
 *
 * @author Keith Ahern
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:04 $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 */
/**
 * @author keith
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class OntologyJRDFMemoryImplUnitTest extends OntologyMemoryImplUnitTest {



  /**
   * The current instance of a database graph.
   */
  protected JRDFGraph graph;

  public static URI ONTO_NAMESPACE1 = null;
  public static URI ONTO_NAMESPACE2 = null;

  static {
      try {
          ONTO_NAMESPACE1 = new URI("http://sofa.org/test/namespace1");
          ONTO_NAMESPACE2 = new URI("http://sofa.org/test/namespace2");
      }
      catch (URISyntaxException e) {
          e.printStackTrace();
      }
  }

  /**
   * The URI of the test graph to create.
   */
  protected URI graphURI;

  /**
   *
   * @param arg0 String
   */
  public OntologyJRDFMemoryImplUnitTest(String arg0) {
    super(arg0);
    // TODO Auto-generated constructor stub

  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    OntologyModel ontoModel = new OntologyJRDFModel();
    onto = OntoConnector.getInstance().createOntology(ontoModel, ONTO_NAMESPACE);
    initOntology();
  }


}
