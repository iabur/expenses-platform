package com.expenses.svcfx.exception;

public class CurrencyNotFoundException extends RuntimeException {
  public CurrencyNotFoundException(String message) {
    super(message);
  }

  public CurrencyNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
