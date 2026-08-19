package com.smartqueue.servicecatalog.controller;

import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.servicecatalog.dto.QueueServiceRequest;
import com.smartqueue.servicecatalog.dto.QueueServiceResponse;
import com.smartqueue.servicecatalog.service.QueueServiceManagementService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
public class QueueServiceController {

  private final QueueServiceManagementService service;

  public QueueServiceController(QueueServiceManagementService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<QueueServiceResponse>> create(
      @Valid @RequestBody QueueServiceRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(service.create(request)));
  }

  @GetMapping
  public ApiResponse<List<QueueServiceResponse>> list(@RequestParam UUID departmentId) {
    return ApiResponse.success(service.list(departmentId));
  }

  @GetMapping("/{id}")
  public ApiResponse<QueueServiceResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(service.getResponse(id));
  }

  @PutMapping("/{id}")
  public ApiResponse<QueueServiceResponse> update(
      @PathVariable UUID id, @Valid @RequestBody QueueServiceRequest request) {
    return ApiResponse.success(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
