package com.expenses.svcexpense.exception;

/**
 * Exception thrown when user tries to access expense without permission
 */
public class UnauthorizedExpenseAccessException extends RuntimeException {

  public UnauthorizedExpenseAccessException(String message) {
    super(message);
  }

  public UnauthorizedExpenseAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
