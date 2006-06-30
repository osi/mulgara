package org.mulgara.sofa.test;

import org.mulgara.sofa.OntologyJRDFModel;

import net.java.dev.sofa.impl.OntoConnector;
import net.java.dev.sofa.model.OntologyModel;

/**
 * TODO ONE LINE DESC <p>
 *
 * TODO MORE DETAILED DESC
 * </p>
 *
 * @created Sep 2, 2004
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
public class ThingJRDFMemoryImplUnitTest extends ThingMemoryImplUnitTest {

  /**
   *
   * @param arg0 String
   */
  public ThingJRDFMemoryImplUnitTest(String arg0) {
    super(arg0);
    // TODO Auto-generated constructor stub
  }

  public static void main(String[] args) {
      junit.textui.TestRunner.run(ThingJRDFMemoryImplUnitTest.class);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
      //onto = OntoConnector.getInstance().createOntology(OntologyMemoryImplUnitTest.ONTO_NAMESPACE);

      onto = OntoConnector.getInstance().createOntology(new OntologyJRDFModel(), OntologyMemoryImplUnitTest.ONTO_NAMESPACE);
      try {
          c = onto.createConcept("class");
          t = onto.createThing(ID, c);
      } catch (Exception ex) {
          fail(ex.getMessage());
      }
      t.setLabel(LABEL);
      t.setComment(COMMENT);
      t.setVersionInfo(VERSIONINFO);
      /*DEBUG dump
      System.out.println("---------");
      ((OntologyMemoryModel)((OntologyImpl)onto).getModel()).dump();
      System.out.println("---------");
      ((OntologyMemoryModel)((OntologyImpl)SOFA.getSystemOntology()).getModel()).dump();
      System.out.println("---------");
      */
  }


}
