package com.smartqueue.common.response;

import java.util.Map;

public record ApiError(String code, String message, Map<String, String> fieldErrors) {

  public static ApiError of(String code, String message) {
    return new ApiError(code, message, Map.of());
  }
}
