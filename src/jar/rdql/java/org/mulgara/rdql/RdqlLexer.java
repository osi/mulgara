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

package org.mulgara.rdql;

// Java 2 standard packages
import java.io.*;
import java.net.*;
import java.util.*;

// Third party packages
import org.apache.log4j.*;

// Automatically generated packages (SableCC)
import org.mulgara.rdql.analysis.*;
import org.mulgara.rdql.lexer.*;
import org.mulgara.rdql.node.EOF;
import org.mulgara.rdql.node.Token;
import org.mulgara.rdql.node.TTerminator;
import org.mulgara.rdql.parser.*;

 /**
 * A lexer that adds command counting capability to the one generated by
 * SableCC.
 *
 * @created 2004-02-04
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:22 $ by $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class RdqlLexer extends Lexer
{
  int commandCount = 0;
  final LinkedList leftoverTokenList = new LinkedList();

  /**
  * the logger
  */
  private final static Logger logger =
    Logger.getLogger(RdqlLexer.class.getName());

  /**
  * Constructor.
  */
  public RdqlLexer()
  {
    super(null);
  }

  public int getCommandCount()
  {
    return commandCount;
  }

  public void add(String command) throws LexerException, IOException
  {
    Lexer lexer = new Lexer(new PushbackReader(new StringReader(command), 256));
    Token t;

    while (! ( (t = lexer.next()) instanceof EOF)) {
      if (t instanceof TTerminator) {
        t = new EOF();
        commandCount++;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Adding token \""+t.getText()+"\" ("+t.getClass()+")");
      }
      leftoverTokenList.add(t);
    }
  }

  /**
  * Discard any unparsed tokens.
  */
  public void flush()
  {
    leftoverTokenList.clear();
  }

  public Token getToken() throws LexerException, IOException
  {
    Token t = super.getToken();
    if (logger.isDebugEnabled()) {
      logger.debug("Got token: \""+t.getText()+"\" ("+t.getClass()+")");
    }
    return t;
  }

  public Token next() throws LexerException, IOException
  {
    Token t = leftoverTokenList.isEmpty()
              ? new EOF()
              : (Token) leftoverTokenList.removeFirst();

    return t;
  }

  public Token peek() throws LexerException, IOException
  {
    return leftoverTokenList.isEmpty()
           ? new EOF()
           : (Token) leftoverTokenList.getFirst();
  }

  public boolean nextCommand()
  {
    if (commandCount == 0) {
      return false;
    }
    else {
      //assert commandCount > 0;
      commandCount--;

      return true;
    }
  }
}
