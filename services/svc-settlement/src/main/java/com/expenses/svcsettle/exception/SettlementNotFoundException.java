package com.expenses.svcsettle.exception;

/**
 * Exception thrown when a settlement proposal or payment is not found
 */
public class SettlementNotFoundException extends RuntimeException {

  public SettlementNotFoundException(String message) {
    super(message);
  }

  public SettlementNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
