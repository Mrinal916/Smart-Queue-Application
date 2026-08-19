package com.smartqueue.queue.service;

import com.smartqueue.queue.config.RedisQueueProperties;
import com.smartqueue.token.enums.TokenStatus;
import com.smartqueue.token.repository.TokenRepository;
import java.time.LocalDate;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rebuilds today's live queue from PostgreSQL, the authoritative store, after application startup.
 */
@Service
public class RedisQueueReconciliationService {
  private static final Logger log = LoggerFactory.getLogger(RedisQueueReconciliationService.class);
  private final TokenRepository tokens;
  private final StringRedisTemplate redis;
  private final RedisQueueProperties properties;

  public RedisQueueReconciliationService(
      TokenRepository tokens, StringRedisTemplate redis, RedisQueueProperties properties) {
    this.tokens = tokens;
    this.redis = redis;
    this.properties = properties;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Transactional(readOnly = true)
  public void reconcileToday() {
    try {
      var active =
          tokens.findAllByQueueDateAndStatusIn(
              LocalDate.now(), EnumSet.of(TokenStatus.WAITING, TokenStatus.SKIPPED));
      for (var token : active) {
        String key =
            properties.keyPrefix()
                + ":"
                + token.getService().getPublicId()
                + ":"
                + token.getQueueDate();
        double score =
            token.hasAgePriority() ? token.getTokenNumber() - 1_000_000d : token.getTokenNumber();
        redis.opsForZSet().add(key, token.getPublicId().toString(), score);
        redis.expire(key, properties.ttl());
      }
      log.info("Reconciled {} active tokens into Redis", active.size());
    } catch (RuntimeException exception) {
      log.error("Redis queue reconciliation failed; PostgreSQL remains authoritative", exception);
    }
  }
}
