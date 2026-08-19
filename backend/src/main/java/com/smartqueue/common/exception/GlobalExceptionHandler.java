package com.smartqueue.common.exception;

import com.smartqueue.auth.exception.AccountDisabledException;
import com.smartqueue.auth.exception.DuplicateEmailException;
import com.smartqueue.auth.exception.InvalidCredentialsException;
import com.smartqueue.common.response.ApiError;
import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.queue.exception.QueueOperationException;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.failure(ApiError.of("NOT_FOUND", exception.getMessage())));
  }

  @ExceptionHandler(BusinessConflictException.class)
  ResponseEntity<ApiResponse<Void>> handleConflict(BusinessConflictException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.failure(ApiError.of("BUSINESS_CONFLICT", exception.getMessage())));
  }

  @ExceptionHandler(QueueOperationException.class)
  ResponseEntity<ApiResponse<Void>> handleQueueOperation(QueueOperationException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.failure(ApiError.of("INVALID_TOKEN_TRANSITION", exception.getMessage())));
  }

  @ExceptionHandler(DuplicateEmailException.class)
  ResponseEntity<ApiResponse<Void>> handleDuplicateEmail(DuplicateEmailException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.failure(ApiError.of("DUPLICATE_EMAIL", exception.getMessage())));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
      InvalidCredentialsException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.failure(ApiError.of("INVALID_CREDENTIALS", exception.getMessage())));
  }

  @ExceptionHandler(AccountDisabledException.class)
  ResponseEntity<ApiResponse<Void>> handleDisabledAccount(AccountDisabledException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.failure(ApiError.of("ACCOUNT_DISABLED", exception.getMessage())));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

    ApiError error =
        new ApiError("VALIDATION_FAILED", "One or more fields are invalid", fieldErrors);
    return ResponseEntity.badRequest().body(ApiResponse.failure(error));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException exception) {
    ApiError error = ApiError.of("VALIDATION_FAILED", exception.getMessage());
    return ResponseEntity.badRequest().body(ApiResponse.failure(error));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
    log.error("Unhandled application exception", exception);
    ApiError error = ApiError.of("INTERNAL_ERROR", "An unexpected error occurred");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.failure(error));
  }
}
