package com.pm.billingservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global handler for any REST endpoints (e.g., actuator) exposed by billing-service.
 * Note: gRPC errors are handled by the gRPC stack itself; this advice ensures any
 * unhandled REST exception is logged with full root cause instead of returning a
 * silent 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception ex) {
    log.error("Unhandled exception", ex);
    Map<String, String> errors = new HashMap<>();
    errors.put("message", "Internal server error");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errors);
  }
}
