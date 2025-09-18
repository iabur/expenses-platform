package com.expenses.svcexpense.exception;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(ExpenseNotFoundException.class)
  public ResponseEntity<ProblemDetail> handleExpenseNotFound(ExpenseNotFoundException ex) {
    log.warn("Expense not found: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND, ex.getMessage());
    problemDetail.setTitle("Expense Not Found");
    problemDetail.setProperty("timestamp", ZonedDateTime.now());
    problemDetail.setProperty("errorCode", "EXPENSE_NOT_FOUND");

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
  }

  @ExceptionHandler(UnauthorizedExpenseAccessException.class)
  public ResponseEntity<ProblemDetail> handleUnauthorizedAccess(UnauthorizedExpenseAccessException ex) {
    log.warn("Unauthorized expense access: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN, ex.getMessage());
    problemDetail.setTitle("Access Denied");
    problemDetail.setProperty("timestamp", ZonedDateTime.now());
    problemDetail.setProperty("errorCode", "UNAUTHORIZED_EXPENSE_ACCESS");

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidationErrors(MethodArgumentNotValidException ex) {
    log.warn("Validation error: {}", ex.getMessage());

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Validation failed");
    problemDetail.setTitle("Invalid Request Data");
    problemDetail.setProperty("timestamp", ZonedDateTime.now());
    problemDetail.setProperty("errorCode", "VALIDATION_FAILED");
    problemDetail.setProperty("validationErrors", errors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Invalid argument: {}", ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, ex.getMessage());
    problemDetail.setTitle("Invalid Request");
    problemDetail.setProperty("timestamp", ZonedDateTime.now());
    problemDetail.setProperty("errorCode", "INVALID_ARGUMENT");

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleGenericError(Exception ex) {
    log.error("Unexpected error occurred", ex);

    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setProperty("timestamp", ZonedDateTime.now());
    problemDetail.setProperty("errorCode", "INTERNAL_ERROR");

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
  }
}
