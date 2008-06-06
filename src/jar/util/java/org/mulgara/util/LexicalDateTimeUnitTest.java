/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/osl-3.0.txt
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 */

package org.mulgara.util;

import java.nio.ByteBuffer;
import java.text.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test cases for LexicalDateTime
 *
 * @created Jun 5, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class LexicalDateTimeUnitTest extends TestCase {

  String[] dtStrings = new String[] {
      "2002-10-10T12:00:12.34-05:00",
      "2002-10-10T12:00:12.34+05:00",
      "2002-10-10T12:00:12.34+00:00",
      "2002-10-10T00:00:12.34+00:00",
      "2002-10-10T24:00:00.00+00:00",
      "2002-10-10T12:00:12.34+10:00",
      "2002-10-10T12:00:12.34Z",
      "2002-10-10T00:00:12.34Z",
      "2002-10-10T24:00:00.00Z",
      "2002-10-10T12:00:12.345-05:00",
      "2002-10-10T12:00:12.345+05:00",
      "2002-10-10T12:00:12.345+00:00",
      "2002-10-10T00:00:12.345+00:00",
      "2002-10-10T24:00:00.000+00:00",
      "2002-10-10T12:00:12.345+10:00",
      "2002-10-10T12:00:12.345Z",
      "2002-10-10T00:00:12.345Z",
      "2002-10-10T24:00:00.000Z"
  };
  
  String[] negStrings = new String[] {
      "-0002-10-10T12:00:12.34-05:00",
      "-0002-10-10T12:00:12.34+05:00",
      "-0002-10-10T12:00:12.34+00:00",
      "-0002-10-10T00:00:12.34+00:00",
      "-0002-10-10T24:00:00.00+00:00",
      "-0002-10-10T12:00:12.34+10:00",
      "-0002-10-10T12:00:12.34Z",
      "-0002-10-10T00:00:12.34Z",
      "-0002-10-10T24:00:00.00Z",
      "-0002-10-10T12:00:12.345-05:00",
      "-0002-10-10T12:00:12.345+05:00",
      "-0002-10-10T12:00:12.345+00:00",
      "-0002-10-10T00:00:12.345+00:00",
      "-0002-10-10T24:00:00.000+00:00",
      "-0002-10-10T12:00:12.345+10:00",
      "-0002-10-10T12:00:12.345Z",
      "-0002-10-10T00:00:12.345Z",
      "-0002-10-10T24:00:00.000Z"
  };

  String[] largeStrings = new String[] {
      "12002-10-10T12:00:12.34-05:00",
      "120002-10-10T12:00:12.34-05:00",
      "-12002-10-10T12:00:12.34-05:00",
      "-120002-10-10T12:00:12.34-05:00"
  };

  public LexicalDateTimeUnitTest(String name) {
    super(name);
  }

  /**
   * Hook for test runner to obtain a test suite from.
   * @return The test suite to run.
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    suite.addTest(new LexicalDateTimeUnitTest("testParseDateTime"));
    suite.addTest(new LexicalDateTimeUnitTest("testEncode"));
    suite.addTest(new LexicalDateTimeUnitTest("testEncodeTimezoneState"));
    return suite;
  }

  /**
   * Default test runner.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  /**
   * Test method for {@link org.mulgara.util.LexicalDateTime#parseDateTime(java.lang.String)}.
   */
  public void testParseDateTime() throws ParseException {
    parseDateTimeHelper(dtStrings);
    parseDateTimeHelper(negStrings);
    parseDateTimeHelper(largeStrings);
  }

  void parseDateTimeHelper(String[] strings) throws ParseException {
    for (String l: strings) {
      LexicalDateTime dt = LexicalDateTime.parseDateTime(l);
      assertEquals(l, dt.toString());
    }
  }

  /**
   * Test method for {@link org.mulgara.util.LexicalDateTime#encode(java.nio.ByteBuffer)}.
   */
  public void testEncode() throws ParseException {
    encodeHelper(dtStrings);
    encodeHelper(negStrings);
    encodeHelper(largeStrings);
  }

  void encodeHelper(String[] strings) throws ParseException {
    for (String l: strings) {
      LexicalDateTime dt = LexicalDateTime.parseDateTime(l);

      ByteBuffer bb = ByteBuffer.allocate(16);
      LexicalDateTime newDt = LexicalDateTime.decode(dt.encode(bb));

      assertEquals(l, newDt.toString());
    }
  }

  /**
   * Test method for {@link org.mulgara.util.LexicalDateTime#encodeTimezoneState()}.
   */
  public void testEncodeTimezoneState() throws ParseException {
    encodeTimezoneStateHelper(dtStrings);
    encodeTimezoneStateHelper(negStrings);
    encodeTimezoneStateHelper(largeStrings);
  }

  void encodeTimezoneStateHelper(String[] strings) throws ParseException {
    for (String l: strings) {
      LexicalDateTime dt = LexicalDateTime.parseDateTime(l);
      long millis = dt.getMillis();
      byte tzstate = dt.encodeTimezoneState();
      byte dec = dt.getDecimalPlaces();

      LexicalDateTime newDt = LexicalDateTime.decode(millis, tzstate, dec);
      assertEquals(l, newDt.toString());
    }
  }
}
