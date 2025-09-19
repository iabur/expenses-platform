package com.expenses.svcuser.exception;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
    log.warn("User not found: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(ZonedDateTime.now())
        .status(HttpStatus.NOT_FOUND.value())
        .error("User Not Found")
        .message(ex.getMessage())
        .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
  }

  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
    log.warn("User already exists: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(ZonedDateTime.now())
        .status(HttpStatus.CONFLICT.value())
        .error("User Already Exists")
        .message(ex.getMessage())
        .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(ZonedDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Invalid Request")
        .message(ex.getMessage())
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    log.warn("Validation error: {}", ex.getMessage());

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(ZonedDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error("Validation Failed")
        .message("Invalid input data")
        .details(errors)
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
    log.error("Unexpected error occurred", ex);

    ErrorResponse error = ErrorResponse.builder()
        .timestamp(ZonedDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error("Internal Server Error")
        .message("An unexpected error occurred")
        .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  // Error response record
  public record ErrorResponse(
      ZonedDateTime timestamp,
      int status,
      String error,
      String message,
      String path,
      Map<String, String> details) {
    public static ErrorResponseBuilder builder() {
      return new ErrorResponseBuilder();
    }

    public static class ErrorResponseBuilder {
      private ZonedDateTime timestamp;
      private int status;
      private String error;
      private String message;
      private String path;
      private Map<String, String> details;

      public ErrorResponseBuilder timestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
      }

      public ErrorResponseBuilder status(int status) {
        this.status = status;
        return this;
      }

      public ErrorResponseBuilder error(String error) {
        this.error = error;
        return this;
      }

      public ErrorResponseBuilder message(String message) {
        this.message = message;
        return this;
      }

      public ErrorResponseBuilder path(String path) {
        this.path = path;
        return this;
      }

      public ErrorResponseBuilder details(Map<String, String> details) {
        this.details = details;
        return this;
      }

      public ErrorResponse build() {
        return new ErrorResponse(timestamp, status, error, message, path, details);
      }
    }
  }
}
