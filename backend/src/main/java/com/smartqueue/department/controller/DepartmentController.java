package com.smartqueue.department.controller;

import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.department.dto.*;
import com.smartqueue.department.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {
  private final DepartmentService service;

  public DepartmentController(DepartmentService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<DepartmentResponse>> create(
      @Valid @RequestBody DepartmentRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(r)));
  }

  @GetMapping
  public ApiResponse<List<DepartmentResponse>> list(@RequestParam UUID officeId) {
    return ApiResponse.success(service.list(officeId));
  }

  @GetMapping("/{id}")
  public ApiResponse<DepartmentResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(service.getResponse(id));
  }

  @PutMapping("/{id}")
  public ApiResponse<DepartmentResponse> update(
      @PathVariable UUID id, @Valid @RequestBody DepartmentRequest r) {
    return ApiResponse.success(service.update(id, r));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
