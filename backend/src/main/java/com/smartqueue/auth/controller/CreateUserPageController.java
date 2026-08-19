package com.smartqueue.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CreateUserPageController {

  @GetMapping("/create-user")
  public String createUserPage() {
    return "redirect:/create-user.html";
  }

  @GetMapping("/reset-password")
  public String resetPasswordPage() {
    return "forward:/index.html";
  }
}
