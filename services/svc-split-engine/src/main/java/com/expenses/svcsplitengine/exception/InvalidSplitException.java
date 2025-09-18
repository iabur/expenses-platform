package com.expenses.svcsplitengine.exception;

/**
 * Exception thrown when split calculation is invalid
 */
public class InvalidSplitException extends RuntimeException {

  public InvalidSplitException(String message) {
    super(message);
  }

  public InvalidSplitException(String message, Throwable cause) {
    super(message, cause);
  }
}
