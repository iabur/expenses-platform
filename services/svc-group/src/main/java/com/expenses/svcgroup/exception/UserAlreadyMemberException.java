package com.expenses.svcgroup.exception;

public class UserAlreadyMemberException extends RuntimeException {
  public UserAlreadyMemberException(String message) {
    super(message);
  }
}
