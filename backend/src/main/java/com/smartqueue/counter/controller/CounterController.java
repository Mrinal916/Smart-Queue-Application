package com.smartqueue.counter.controller;

import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.counter.dto.*;
import com.smartqueue.counter.service.CounterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/counters")
public class CounterController {
  private final CounterService service;

  public CounterController(CounterService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CounterResponse>> create(
      @Valid @RequestBody CounterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(service.create(request)));
  }

  @GetMapping
  public ApiResponse<List<CounterResponse>> list(@RequestParam UUID officeId) {
    return ApiResponse.success(service.list(officeId));
  }

  @GetMapping("/management")
  public ApiResponse<List<CounterManagementResponse>> managementList(@RequestParam UUID officeId) {
    return ApiResponse.success(service.managementList(officeId));
  }

  @GetMapping("/officer-assignment-history")
  public ApiResponse<List<OfficerAssignmentHistoryResponse>> officerAssignmentHistory(
      @RequestParam UUID officeId) {
    return ApiResponse.success(service.officerAssignmentHistory(officeId));
  }

  @GetMapping("/service-assignments")
  public ApiResponse<List<CounterServiceAssignmentResponse>> serviceAssignments(
      @RequestParam UUID officeId) {
    return ApiResponse.success(service.serviceAssignments(officeId));
  }

  @GetMapping("/operation-options")
  public ApiResponse<List<OfficerCounterOptionResponse>> operationOptions() {
    return ApiResponse.success(service.operationOptions());
  }

  @GetMapping("/{id}")
  public ApiResponse<CounterResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(service.getResponse(id));
  }

  @PutMapping("/{id}")
  public ApiResponse<CounterResponse> update(
      @PathVariable UUID id, @Valid @RequestBody CounterRequest request) {
    return ApiResponse.success(service.update(id, request));
  }

  @PostMapping("/{id}/open")
  public ApiResponse<CounterResponse> open(@PathVariable UUID id) {
    return ApiResponse.success(service.open(id));
  }

  @PostMapping("/{id}/close")
  public ApiResponse<CounterResponse> close(@PathVariable UUID id) {
    return ApiResponse.success(service.close(id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }

  @PostMapping("/officer-assignments")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Void> assignOfficer(@Valid @RequestBody OfficerAssignmentRequest request) {
    service.assignOfficer(request);
    return ApiResponse.success(null);
  }

  @DeleteMapping("/{counterId}/officer-assignment")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void releaseOfficer(@PathVariable UUID counterId) {
    service.releaseOfficer(counterId);
  }

  @PostMapping("/service-assignments")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<Void> assignService(
      @Valid @RequestBody CounterServiceAssignmentRequest request) {
    service.assignService(request);
    return ApiResponse.success(null);
  }

  @DeleteMapping("/{counterId}/service-assignments/{serviceId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void releaseService(@PathVariable UUID counterId, @PathVariable UUID serviceId) {
    service.releaseService(counterId, serviceId);
  }
}
