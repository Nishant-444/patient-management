package com.pm.patientservice.exception;

public class EventPublishingException extends RuntimeException {
  public EventPublishingException(String message, Throwable cause) {
    super(message, cause);
  }
}
