package com.expenses.svcexpense.exception;

/**
 * Exception thrown when an expense is not found
 */
public class ExpenseNotFoundException extends RuntimeException {

  public ExpenseNotFoundException(String message) {
    super(message);
  }

  public ExpenseNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
