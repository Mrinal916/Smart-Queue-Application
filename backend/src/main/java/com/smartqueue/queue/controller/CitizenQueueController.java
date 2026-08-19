package com.smartqueue.queue.controller;

import com.smartqueue.common.response.ApiResponse;
import com.smartqueue.queue.service.QueueEngineService;
import com.smartqueue.token.dto.LiveQueueStatusResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/queue")
public class CitizenQueueController {
  private final QueueEngineService engine;

  public CitizenQueueController(QueueEngineService engine) {
    this.engine = engine;
  }

  @GetMapping("/live-status")
  public ApiResponse<LiveQueueStatusResponse> liveStatus(@RequestParam UUID serviceId) {
    return ApiResponse.success(engine.liveStatus(serviceId));
  }
}
