package com.smartqueue.counter.controller;

import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.counter.dto.CounterResponse;
import com.smartqueue.counter.dto.OfficerCounterOptionResponse;
import com.smartqueue.counter.dto.OfficerDashboardResponse;
import com.smartqueue.counter.dto.QueueSummaryResponse;
import com.smartqueue.counter.service.CounterService;
import com.smartqueue.counter.service.OfficerMonitoringService;
import com.smartqueue.token.enums.TokenStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/officer/counters")
public class OfficerCounterController {
  private final CounterService counters;
  private final OfficerMonitoringService monitoring;

  public OfficerCounterController(CounterService counters, OfficerMonitoringService monitoring) {
    this.counters = counters;
    this.monitoring = monitoring;
  }

  @GetMapping
  public ApiResponse<List<OfficerCounterOptionResponse>> assigned(Authentication authentication) {
    return ApiResponse.success(
        monitoring.assignedCounters(UUID.fromString(authentication.getName())));
  }

  @GetMapping("/{counterId}/status")
  public ApiResponse<CounterResponse> status(
      Authentication authentication, @PathVariable UUID counterId) {
    monitoring.authorizeCounter(UUID.fromString(authentication.getName()), counterId);
    return ApiResponse.success(counters.getResponse(counterId));
  }

  @GetMapping("/{counterId}/queue")
  public ApiResponse<QueueSummaryResponse> queue(
      Authentication authentication, @PathVariable UUID counterId, @RequestParam UUID serviceId) {
    return ApiResponse.success(
        monitoring.summary(UUID.fromString(authentication.getName()), counterId, serviceId));
  }

  @GetMapping("/{counterId}/dashboard")
  public ApiResponse<OfficerDashboardResponse> dashboard(
      Authentication authentication,
      @PathVariable UUID counterId,
      @RequestParam UUID serviceId,
      @RequestParam LocalDate date,
      @RequestParam(required = false) Set<TokenStatus> statuses,
      @RequestParam(required = false) Boolean arrived) {
    return ApiResponse.success(
        monitoring.dashboard(
            UUID.fromString(authentication.getName()),
            counterId,
            serviceId,
            date,
            statuses,
            arrived));
  }

  @PostMapping("/{counterId}/open")
  public ApiResponse<CounterResponse> open(
      Authentication authentication, @PathVariable UUID counterId) {
    monitoring.authorizeCounter(UUID.fromString(authentication.getName()), counterId);
    return ApiResponse.success(counters.open(counterId));
  }

  @PostMapping("/{counterId}/close")
  public ApiResponse<CounterResponse> close(
      Authentication authentication, @PathVariable UUID counterId) {
    monitoring.authorizeCounter(UUID.fromString(authentication.getName()), counterId);
    return ApiResponse.success(counters.close(counterId));
  }
}
