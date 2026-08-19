package com.smartqueue.common.exception;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String resource, Object id) {
    super(resource + " was not found: " + id);
  }
}
