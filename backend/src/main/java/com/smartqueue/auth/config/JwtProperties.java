package com.smartqueue.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartqueue.security.jwt")
public record JwtProperties(String secret, Duration expiration, Duration qrExpiration) {
  public JwtProperties {
    if (secret == null
        || secret.isBlank()
        || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException(
          "JWT secret must be configured and contain at least 32 bytes");
    }
    if (expiration == null || expiration.isNegative() || expiration.isZero()) {
      throw new IllegalStateException("JWT access-token expiration must be positive");
    }
    if (qrExpiration == null || qrExpiration.isNegative() || qrExpiration.isZero()) {
      throw new IllegalStateException("JWT QR expiration must be positive");
    }
  }
}
