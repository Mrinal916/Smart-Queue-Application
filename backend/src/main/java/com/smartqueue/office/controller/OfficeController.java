package com.smartqueue.office.controller;

import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.office.dto.*;
import com.smartqueue.office.service.OfficeService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/offices")
public class OfficeController {
  private final OfficeService service;

  public OfficeController(OfficeService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<OfficeResponse>> create(@Valid @RequestBody OfficeRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.create(r)));
  }

  @GetMapping
  public ApiResponse<List<OfficeResponse>> list() {
    return ApiResponse.success(service.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<OfficeResponse> get(@PathVariable UUID id) {
    return ApiResponse.success(service.getResponse(id));
  }

  @PutMapping("/{id}")
  public ApiResponse<OfficeResponse> update(
      @PathVariable UUID id, @Valid @RequestBody OfficeRequest r) {
    return ApiResponse.success(service.update(id, r));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
