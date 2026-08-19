package com.smartqueue.queue.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RedisQueueProperties.class)
public class RedisQueueConfig {}
