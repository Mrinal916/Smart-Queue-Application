package com.smartqueue.user.service;

import com.smartqueue.common.exception.ResourceNotFoundException;
import com.smartqueue.token.dto.TokenPageResponse;
import com.smartqueue.token.dto.TokenResponse;
import com.smartqueue.token.entity.Token;
import com.smartqueue.token.repository.TokenRepository;
import com.smartqueue.user.dto.UpdateUserRoleRequest;
import com.smartqueue.user.dto.UserSummaryResponse;
import com.smartqueue.user.entity.UserAccount;
import com.smartqueue.user.enums.RoleName;
import com.smartqueue.user.enums.TokenActivity;
import com.smartqueue.user.repository.RoleRepository;
import com.smartqueue.user.repository.UserAccountRepository;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDirectoryService {

  private final UserAccountRepository users;
  private final TokenRepository tokens;
  private final RoleRepository roles;

  public UserDirectoryService(
      UserAccountRepository users, TokenRepository tokens, RoleRepository roles) {
    this.users = users;
    this.tokens = tokens;
    this.roles = roles;
  }

  @Transactional(readOnly = true)
  public List<UserSummaryResponse> listUsers() {
    return users.findAllByOrderByEmailAsc().stream()
        .sorted(
            Comparator.comparingInt((UserAccount user) -> roleOrder(user.getRole().getName()))
                .thenComparing(UserAccount::getEmail))
        .map(this::mapUser)
        .toList();
  }

  @Transactional(readOnly = true)
  public TokenPageResponse tokenHistory(UUID userId, int page, int size) {
    UserAccount user =
        users
            .findByPublicId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    if (user.getRole().getName() != RoleName.CITIZEN) {
      throw new com.smartqueue.common.exception.BusinessConflictException(
          "Token history is available only for citizen accounts");
    }
    var result =
        tokens.findAllByCitizenIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));
    return new TokenPageResponse(
        result.getContent().stream().map(this::mapToken).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional(readOnly = true)
  public TokenPageResponse allTokenHistory(TokenActivity activity, int page, int size) {
    var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    var activeStatuses =
        EnumSet.of(
            com.smartqueue.token.enums.TokenStatus.WAITING,
            com.smartqueue.token.enums.TokenStatus.CALLED,
            com.smartqueue.token.enums.TokenStatus.SKIPPED);
    var inactiveStatuses =
        EnumSet.of(
            com.smartqueue.token.enums.TokenStatus.COMPLETED,
            com.smartqueue.token.enums.TokenStatus.NO_SHOW,
            com.smartqueue.token.enums.TokenStatus.CANCELLED);
    var result =
        activity == TokenActivity.ALL
            ? tokens.findAll(pageable)
            : tokens.findAllByStatusIn(
                activity == TokenActivity.ACTIVE ? activeStatuses : inactiveStatuses, pageable);
    return new TokenPageResponse(
        result.getContent().stream().map(this::mapToken).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional
  public UserSummaryResponse setUserEnabled(UUID userId, boolean enabled, UUID administratorId) {
    UserAccount user =
        users
            .findByPublicId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    if (user.getPublicId().equals(administratorId)) {
      throw new com.smartqueue.common.exception.BusinessConflictException(
          "You cannot disable your own administrator account");
    }
    if (user.getRole().getName() == RoleName.ADMIN) {
      throw new com.smartqueue.common.exception.BusinessConflictException(
          "Administrator accounts cannot be disabled");
    }
    user.setEnabled(enabled);
    return mapUser(user);
  }

  @Transactional
  public UserSummaryResponse updateRole(
      UUID userId, UpdateUserRoleRequest request, UUID administratorId) {
    UserAccount user =
        users
            .findByPublicId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    if (user.getPublicId().equals(administratorId)) {
      throw new com.smartqueue.common.exception.BusinessConflictException(
          "You cannot change your own administrator role");
    }
    if (user.getRole().getName() == request.role()) {
      throw new com.smartqueue.common.exception.BusinessConflictException(
          "User is already assigned the " + request.role() + " role");
    }
    user.setRole(
        roles
            .findByName(request.role())
            .orElseThrow(() -> new ResourceNotFoundException("Role", request.role())));
    return mapUser(user);
  }

  private UserSummaryResponse mapUser(UserAccount user) {
    return new UserSummaryResponse(
        user.getPublicId(), user.getEmail(), user.getRole().getName(), user.isEnabled());
  }

  private int roleOrder(RoleName role) {
    return switch (role) {
      case ADMIN -> 0;
      case OFFICER -> 1;
      case CITIZEN -> 2;
    };
  }

  private TokenResponse mapToken(Token token) {
    return new TokenResponse(
        token.getPublicId(),
        token.getTokenNumber(),
        token.getQueueDate(),
        token.getAppointmentTime(),
        token.getStatus(),
        token.getService().getPublicId(),
        token.getOffice().getPublicId(),
        token.getOffice().getName(),
        token.getOffice().getAddress(),
        token.getService().getDepartment().getName(),
        token.getService().getName(),
        token.getVisitorName(),
        token.getVisitorPhone(),
        token.getVisitorAge(),
        token.getVisitorGender(),
        token.hasAgePriority(),
        token.hasAppeared(),
        token.getAppearedAt(),
        token.getCounter() == null ? null : token.getCounter().getPublicId(),
        token.getCounter() == null ? null : token.getCounter().getCode());
  }
}
