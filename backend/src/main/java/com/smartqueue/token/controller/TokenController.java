package com.smartqueue.token.controller;

import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.queue.dto.CounterOperationRequest;
import com.smartqueue.queue.dto.TokenOperationRequest;
import com.smartqueue.queue.service.QueueEngineService;
import com.smartqueue.token.dto.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tokens")
public class TokenController {
  private final QueueEngineService engine;

  public TokenController(QueueEngineService engine) {
    this.engine = engine;
  }

  @PostMapping
  public ApiResponse<TokenResponse> book(Authentication a, @Valid @RequestBody BookTokenRequest r) {
    return ApiResponse.success(
        engine.book(
            UUID.fromString(a.getName()),
            r.visitorName(),
            r.visitorPhone(),
            r.visitorAge(),
            r.visitorGender(),
            r.serviceId(),
            r.appointmentDate(),
            r.appointmentTime(),
            r.idempotencyKey()));
  }

  @GetMapping("/active")
  public ApiResponse<TokenResponse> active(Authentication a) {
    return ApiResponse.success(engine.activeToken(UUID.fromString(a.getName())));
  }

  @GetMapping("/history")
  public ApiResponse<TokenPageResponse> history(
      Authentication a,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ApiResponse.success(
        engine.history(UUID.fromString(a.getName()), page, Math.min(size, 100)));
  }

  @GetMapping("/available-slots")
  public ApiResponse<List<LocalTime>> availableSlots(
      @RequestParam UUID serviceId, @RequestParam LocalDate appointmentDate) {
    return ApiResponse.success(engine.availableSlots(serviceId, appointmentDate));
  }

  @GetMapping("/{id}")
  public ApiResponse<TokenResponse> details(Authentication a, @PathVariable UUID id) {
    return ApiResponse.success(engine.checkIn(UUID.fromString(a.getName()), id));
  }

  @PostMapping("/{id}/arrive")
  public ApiResponse<TokenResponse> arrive(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody TokenOperationRequest r) {
    return ApiResponse.success(engine.markArrived(UUID.fromString(a.getName()), r.counterId(), id));
  }

  @GetMapping("/{id}/wait-time")
  public ApiResponse<WaitTimeResponse> waitTime(Authentication a, @PathVariable UUID id) {
    return ApiResponse.success(engine.waitTime(UUID.fromString(a.getName()), id));
  }

  @GetMapping("/{id}/qr")
  public ApiResponse<QrCodeResponse> qr(Authentication a, @PathVariable UUID id) {
    return ApiResponse.success(engine.qrCode(UUID.fromString(a.getName()), id));
  }

  @PostMapping("/qr/check-in")
  public ApiResponse<TokenResponse> qrCheckIn(
      Authentication a, @Valid @RequestBody QrCheckInRequest r) {
    return ApiResponse.success(engine.validateQrCheckIn(UUID.fromString(a.getName()), r.payload()));
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<TokenResponse> cancel(Authentication a, @PathVariable UUID id) {
    return ApiResponse.success(engine.cancel(UUID.fromString(a.getName()), id));
  }

  @PostMapping("/next")
  public ApiResponse<TokenResponse> next(
      Authentication a, @Valid @RequestBody CounterOperationRequest r) {
    return ApiResponse.success(
        engine.next(UUID.fromString(a.getName()), r.counterId(), r.serviceId()));
  }

  @PostMapping("/{id}/skip")
  public ApiResponse<TokenResponse> skip(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody TokenOperationRequest r) {
    return ApiResponse.success(engine.skip(UUID.fromString(a.getName()), r.counterId(), id));
  }

  @PostMapping("/{id}/recall")
  public ApiResponse<TokenResponse> recall(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody TokenOperationRequest r) {
    return ApiResponse.success(engine.recall(UUID.fromString(a.getName()), r.counterId(), id));
  }

  @PostMapping("/{id}/complete")
  public ApiResponse<TokenResponse> complete(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody TokenOperationRequest r) {
    return ApiResponse.success(engine.complete(UUID.fromString(a.getName()), r.counterId(), id));
  }

  @PostMapping("/{id}/no-show")
  public ApiResponse<TokenResponse> noShow(
      Authentication a, @PathVariable UUID id, @Valid @RequestBody TokenOperationRequest r) {
    return ApiResponse.success(engine.noShow(UUID.fromString(a.getName()), r.counterId(), id));
  }
}
