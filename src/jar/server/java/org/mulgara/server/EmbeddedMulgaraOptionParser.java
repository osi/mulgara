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

package org.mulgara.server;

// third party packages
import jargs.gnu.CmdLineParser;

// third party packages
import org.apache.log4j.Category;

/**
 * Command line option parser for the Mulgara server.
 *
 * @created 2001-12-21
 *
 * @author Tom Adams
 *
 * @modified $Date: 2005/01/13 01:55:32 $ by $Author: raboczi $
 *
 * @maintenanceAuthor $Author: raboczi $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2001-2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class EmbeddedMulgaraOptionParser extends CmdLineParser {

  /**
   * option to display the help
   */
  public final static Option HELP =
    new CmdLineParser.Option.BooleanOption('h', "help");

  /**
   * option to use an external logging configuration file
   */
  public final static Option LOG_CONFIG =
    new CmdLineParser.Option.StringOption('l', "logconfig");

  /**
   * option to use an external configuration file
   */
  public final static Option SERVER_CONFIG =
    new CmdLineParser.Option.StringOption('c', "serverconfig");

  /**
   * option to change the host the HTTP server is bound to
   */
  public final static Option HTTP_HOST =
    new CmdLineParser.Option.StringOption('o', "httphost");

  /**
   * option to change the host the server is bound to
   */
  public final static Option SERVER_HOST =
      new CmdLineParser.Option.StringOption('k', "serverhost");

  /**
   * option to disble the automatic starting of a RMI Registry
   */
  public final static Option NO_RMI =
      new CmdLineParser.Option.BooleanOption('n', "normi");

  /**
   * option to change to the port the RMI registry listens on
   */
  public final static Option RMI_PORT =
      new CmdLineParser.Option.StringOption('r', "rmiport");

  /**
   * option to change the port the server is bound to
   */
  public final static Option PORT =
      new CmdLineParser.Option.StringOption('p', "port");

  /**
   * option to change the name of the server
   */
  public final static Option SERVER_NAME =
      new CmdLineParser.Option.StringOption('s', "servername");

  /**
   * option to change the location of database files
   */
  public final static Option PERSISTENCE_PATH =
      new CmdLineParser.Option.StringOption('a', "path");

  /**
   * option to set the smtp server
   */
  public final static Option SMTP_SERVER =
      new CmdLineParser.Option.StringOption('m', "smtp");

  //
  // Constants
  //

  /**
   * the category to log to
   */
  private final static Category log =
      Category.getInstance(EmbeddedMulgaraOptionParser.class.getName());

  //
  // members
  //

  /**
   * the command line arguments passed to the Mulgara server
   */
  protected String[] args = null;

  /**
   * flag indicating whether we've parsed the options yet
   */
  protected boolean optionsParsed = false;

  //
  // Constructors
  //

  /**
   * Creates a new Mulgara server command line option parser to parse the command
   * line <code>args</code> given.
   *
   * @param args the command line arguments
   */
  public EmbeddedMulgaraOptionParser(String[] args) {

    // call the superclass constructor
    super();

    // validate args parameter
    if (args == null) {

      throw new IllegalArgumentException("Null \"args\" parameter");
    }

    // end if
    // log that we've created a parser
    //log.debug("Created option parser for Mulgara server");
    // set the member
    this.setArgs(args);

    // add the options
    this.addOption(HELP);
    this.addOption(LOG_CONFIG);
    this.addOption(HTTP_HOST);
    this.addOption(SERVER_HOST);
    this.addOption(PORT);
    this.addOption(NO_RMI);
    this.addOption(RMI_PORT);
    this.addOption(SERVER_NAME);
    this.addOption(PERSISTENCE_PATH);
    this.addOption(SMTP_SERVER);
    this.addOption(SERVER_CONFIG);

  } // EmbeddedMulgaraOptionParser()

  //
  // Public API
  //

  /**
   * Parses the command line arguments given to this parser.
   *
   * @throws UnknownOptionException if an unknown option was specified in the
   *      list of options given to the parser
   * @throws IllegalOptionValueException if an option given to the parser
   *      contains an illegal value
   */
  public void parse() throws UnknownOptionException,
      IllegalOptionValueException {

    // log that
    //log.debug("Parsing Mulgara server arguments");
    // parse the arguments if we haven't done so already
    if (!this.optionsParsed) {

      // parse the arguments
      this.parse(this.getArgs());

      // fail if there are any remaing that we dodn't know about
      String[] remainingArgs = this.getRemainingArgs();

      if (remainingArgs.length > 0) {

        // throw a new exception to indicate that there were unkown arguments
        throw new UnknownOptionException(remainingArgs[0]);
      } // end if

      // we've now parser the options
      this.optionsParsed = true;
    } // end if

  } // parse()

  /**
   * Validates the command line options.
   *
   * @return true if the options are valid
   */
  public boolean optionsValid() {

    boolean optionsValid = false;

    try {

      // validate params by parsing the arguments
      this.parse();
      optionsValid = true;
    }
    catch (UnknownOptionException uoe) {

      optionsValid = false;
    }
    catch (IllegalOptionValueException iove) {

      optionsValid = false;
    }

    // try-catch
    // return whether the options are valid
    return optionsValid;
  } // optionsValid()

  //
  // Internal methods
  //

  /**
   * Sets the command line arguments passed to the itql interpreter.
   *
   * @param args The new Args value
   */
  protected void setArgs(String[] args) {

    this.args = args;
  } // setArgs()

  /**
   * Returns the command line arguments passed to the itql interpreter.
   *
   * @return the command line arguments passed to the itql interpreter
   */
  protected String[] getArgs() {

    return this.args;
  } // getArgs()
}
