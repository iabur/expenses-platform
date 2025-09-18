package com.expenses.svcgroup.exception;

public class GroupNotFoundException extends RuntimeException {
  public GroupNotFoundException(String message) {
    super(message);
  }
}
