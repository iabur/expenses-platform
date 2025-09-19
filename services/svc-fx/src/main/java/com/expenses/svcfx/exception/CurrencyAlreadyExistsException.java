package com.expenses.svcfx.exception;

public class CurrencyAlreadyExistsException extends RuntimeException {
  public CurrencyAlreadyExistsException(String message) {
    super(message);
  }

  public CurrencyAlreadyExistsException(String message, Throwable cause) {
    super(message, cause);
  }
}
