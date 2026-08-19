package com.smartqueue.auth.controller;

import com.smartqueue.auth.dto.request.ForgotPasswordRequest;
import com.smartqueue.auth.dto.request.LoginRequest;
import com.smartqueue.auth.dto.request.RegisterRequest;
import com.smartqueue.auth.dto.request.ResetPasswordRequest;
import com.smartqueue.auth.dto.response.AuthResponse;
import com.smartqueue.auth.service.AuthService;
import com.smartqueue.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<AuthResponse>> register(
      @Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(authService.register(request)));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
  }

  @PostMapping("/forgot-password")
  public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.requestPasswordReset(request);
    return ApiResponse.success(null);
  }

  @PostMapping("/reset-password")
  public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request);
    return ApiResponse.success(null);
  }
}
