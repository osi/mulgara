/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.rosenlaw.com/OSL3.0.htm
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * This file is an original work developed by Netymon Pty Ltd
 * (http://www.netymon.com, mailto:mail@netymon.com). Portions created
 * by Netymon Pty Ltd are Copyright (c) 2006 Netymon Pty Ltd.
 * All Rights Reserved.
 */

package org.mulgara.resolver;

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.query.Answer;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;

/**
 * A transactional answer.  
 * Wraps all calls to the enclosed answer object, ensuring all calls are made
 * within an activated transactional context.  Also ensures that that context
 * is deactivated upon returning from the outer-call.
 *
 * @created 2006-10-06
 *
 * @author <a href="mailto:andrae@netymon.com">Andrae Muys</a>
 *
 * @version $Revision: $
 *
 * @modified $Date: $
 *
 * @maintenanceAuthor $Author: $
 *
 * @company <A href="mailto:mail@netymon.com">Netymon Pty Ltd</A>
 *
 * @copyright &copy;2006 <a href="http://www.netymon.com/">Netymon Pty Ltd</a>
 *
 * @licence Open Software License v3.0</a>
 */

public class TransactionalAnswer implements Answer {
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(MulgaraTransaction.class.getName());

  private Answer answer;

  private MulgaraTransaction transaction;

  public TransactionalAnswer(MulgaraTransaction transaction, Answer answer) throws TuplesException {
    try {
      report("Creating Answer");

      this.answer = answer;
      this.transaction = transaction;
      transaction.reference();

      report("Created Answer");
    } catch (MulgaraTransactionException em) {
      throw new TuplesException("Failed to associate with transaction", em);
    }
  }

  public Object getObject(final int column) throws TuplesException {
    return transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          returnObject(answer.getObject(column));
        }
      }).getObject();
  }

  public Object getObject(final String columnName) throws TuplesException {
    return transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          returnObject(answer.getObject(columnName));
        }
      });
  }

  public void beforeFirst() throws TuplesException {
    transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          answer.beforeFirst();
        }
      });
  }

  public void close() throws TuplesException {
    report("Closing Answer");
    try {
      transaction.execute(new AnswerOperation() {
          public void execute() throws TuplesException {
            answer.close();
            try {
              transaction.dereference();
            } catch (MulgaraTransactionException em) {
              throw new TuplesException("Error dereferencing transaction", em);
            }
          }
        });
    } finally {
      // !!FIXME: Note - We will need to add checks for null to all operations.
      transaction = null;
      answer = null;    // Note this permits the gc of the answer.
      report("Closed Answer");
    }
  }

  public int getColumnIndex(final Variable column) throws TuplesException {
    return transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          returnInt(answer.getColumnIndex(column));
        }
      }).getInt();
  }

  public int getNumberOfVariables() {
    try {
      return transaction.execute(new AnswerOperation() {
          public void execute() {
            returnInt(answer.getNumberOfVariables());
          }
        }).getInt();
    } catch (TuplesException et) {
      throw new IllegalStateException("Doesn't throw TuplesException", et);
    }
  }

  public Variable[] getVariables() {
    try {
      return (Variable[])(transaction.execute(new AnswerOperation() {
          public void execute() {
            returnObject(answer.getVariables());
          }
        }).getObject());
    } catch (TuplesException et) {
      throw new IllegalStateException("Doesn't throw TuplesException", et);
    }
  }

  public boolean isUnconstrained() throws TuplesException {
    return transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          returnBoolean(answer.isUnconstrained());
        }
      }).getBoolean();
  }

  public long getRowCount() throws TuplesException {
    return transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          returnLong(answer.getRowCount());
        }
      }).getLong();
  }

  public long getRowUpperBound() throws TuplesException {
    return transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          returnLong(answer.getRowUpperBound());
        }
      }).getLong();
  }

  public int getRowCardinality() throws TuplesException {
    return transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          returnInt(answer.getRowCardinality());
        }
      }).getInt();
  }

  public boolean next() throws TuplesException {
    return transaction.execute(new AnswerOperation() {
        public void execute() throws TuplesException {
          returnBoolean(answer.next());
        }
      }).getBoolean();
  }

  public Object clone() {
    try {
      TransactionalAnswer c = (TransactionalAnswer)super.clone();
      c.answer = (Answer)this.answer.clone();
      c.transaction.reference();

      return c;
    } catch (CloneNotSupportedException ec) {
      throw new IllegalStateException("Clone failed on Cloneable");
    } catch (MulgaraTransactionException em) {
      throw new IllegalStateException("Failed to associate with transaction", em);
    }
  }

  private void report(String desc) {
    if (logger.isInfoEnabled()) {
      logger.info(desc + ": " + System.identityHashCode(this) + ", xa=" + System.identityHashCode(transaction));
    }
  }

  public void finalize() {
    report("GC-finalizing");
    if (transaction != null) {
      logger.error("TransactionalAnswer not closed");
    }
  }


  void sessionClose() throws TuplesException {
    if (answer != null) {
      report("Session forced close");
      close();
    }
  }
}
