package com.smartqueue.queue.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.smartqueue.queue.config.RedisQueueProperties;
import com.smartqueue.servicecatalog.entity.QueueService;
import com.smartqueue.token.entity.Token;
import com.smartqueue.token.repository.TokenRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
class RedisQueueReconciliationServiceTests {
  @Mock TokenRepository tokens;
  @Mock StringRedisTemplate redis;
  @Mock RedisQueueProperties properties;
  @Mock ZSetOperations<String, String> zset;
  @Mock Token token;
  @Mock QueueService service;
  @InjectMocks RedisQueueReconciliationService reconciliation;

  @Test
  void rebuildsWaitingQueueAndSetsTtl() {
    UUID serviceId = UUID.randomUUID(), tokenId = UUID.randomUUID();
    LocalDate today = LocalDate.now();
    when(tokens.findAllByQueueDateAndStatusIn(eq(today), any())).thenReturn(List.of(token));
    when(token.getService()).thenReturn(service);
    when(service.getPublicId()).thenReturn(serviceId);
    when(token.getPublicId()).thenReturn(tokenId);
    when(token.getQueueDate()).thenReturn(today);
    when(token.getTokenNumber()).thenReturn(7);
    when(properties.keyPrefix()).thenReturn("smartqueue:queue");
    when(properties.ttl()).thenReturn(Duration.ofHours(36));
    when(redis.opsForZSet()).thenReturn(zset);
    reconciliation.reconcileToday();
    String key = "smartqueue:queue:" + serviceId + ":" + today;
    verify(zset).add(key, tokenId.toString(), 7d);
    verify(redis).expire(key, Duration.ofHours(36));
  }
}
