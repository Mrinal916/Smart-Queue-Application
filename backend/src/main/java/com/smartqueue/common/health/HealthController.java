package com.smartqueue.common.health;

import com.smartqueue.common.response.ApiResponse;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

  @GetMapping
  public ResponseEntity<ApiResponse<Map<String, String>>> health() {
    return ResponseEntity.ok(ApiResponse.success(Map.of("service", "smartqueue", "state", "UP")));
  }
}
