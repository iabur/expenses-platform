package com.expenses.svcsettle.exception;

/**
 * Exception thrown when user tries to access settlement without permission
 */
public class UnauthorizedSettlementAccessException extends RuntimeException {

  public UnauthorizedSettlementAccessException(String message) {
    super(message);
  }

  public UnauthorizedSettlementAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
