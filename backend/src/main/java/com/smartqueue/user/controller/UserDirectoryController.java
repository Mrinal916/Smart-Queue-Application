package com.smartqueue.user.controller;

import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.token.dto.TokenPageResponse;
import com.smartqueue.user.dto.UpdateUserRoleRequest;
import com.smartqueue.user.dto.UserSummaryResponse;
import com.smartqueue.user.enums.TokenActivity;
import com.smartqueue.user.service.UserDirectoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserDirectoryController {

  private final UserDirectoryService users;

  public UserDirectoryController(UserDirectoryService users) {
    this.users = users;
  }

  @GetMapping
  public ApiResponse<List<UserSummaryResponse>> list() {
    return ApiResponse.success(users.listUsers());
  }

  @GetMapping("/tokens")
  public ApiResponse<TokenPageResponse> allTokenHistory(
      @RequestParam(defaultValue = "ALL") TokenActivity activity,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    return ApiResponse.success(users.allTokenHistory(activity, page, Math.min(size, 100)));
  }

  @GetMapping("/{userId}/tokens")
  public ApiResponse<TokenPageResponse> tokenHistory(
      @PathVariable UUID userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "100") int size) {
    return ApiResponse.success(users.tokenHistory(userId, page, Math.min(size, 100)));
  }

  @PostMapping("/{userId}/disable")
  public ApiResponse<UserSummaryResponse> disable(
      @PathVariable UUID userId, Authentication authentication) {
    return ApiResponse.success(
        users.setUserEnabled(userId, false, UUID.fromString(authentication.getName())));
  }

  @PostMapping("/{userId}/enable")
  public ApiResponse<UserSummaryResponse> enable(
      @PathVariable UUID userId, Authentication authentication) {
    return ApiResponse.success(
        users.setUserEnabled(userId, true, UUID.fromString(authentication.getName())));
  }

  @PutMapping("/{userId}/role")
  public ApiResponse<UserSummaryResponse> updateRole(
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserRoleRequest request,
      Authentication authentication) {
    return ApiResponse.success(
        users.updateRole(userId, request, UUID.fromString(authentication.getName())));
  }
}
