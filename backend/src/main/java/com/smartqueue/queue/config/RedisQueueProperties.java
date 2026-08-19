package com.smartqueue.queue.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartqueue.queue")
public record RedisQueueProperties(String keyPrefix, Duration ttl) {}
