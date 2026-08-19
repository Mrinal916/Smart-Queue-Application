package com.smartqueue.common.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Prevent browser favicon requests from being reported as application failures. */
@Controller
public class FaviconController {

  @GetMapping("/favicon.ico")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void favicon() {
    // The application does not currently ship a custom favicon.
  }
}
