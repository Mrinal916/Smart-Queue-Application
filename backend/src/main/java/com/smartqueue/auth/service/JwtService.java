package com.smartqueue.auth.service;

import com.smartqueue.auth.config.JwtProperties;
import com.smartqueue.user.entity.UserAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtProperties properties;
  private final Key signingKey;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
    byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      throw new IllegalStateException("JWT secret must be at least 32 bytes");
    }
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  public String createAccessToken(UserAccount user) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(user.getPublicId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRole().getName().name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(properties.expiration())))
        .signWith(signingKey)
        .compact();
  }

  public String extractSubject(String token) {
    return Jwts.parser()
        .verifyWith((javax.crypto.SecretKey) signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public Instant expiresAt() {
    return Instant.now().plus(properties.expiration());
  }

  public Instant qrExpiresAt() {
    return Instant.now().plus(properties.qrExpiration());
  }

  public String createQrCheckInToken(java.util.UUID tokenId) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(tokenId.toString())
        .claim("purpose", "QR_CHECK_IN")
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(properties.qrExpiration())))
        .signWith(signingKey)
        .compact();
  }

  public java.util.UUID extractQrCheckInTokenId(String qrToken) {
    var claims =
        Jwts.parser()
            .verifyWith((javax.crypto.SecretKey) signingKey)
            .build()
            .parseSignedClaims(qrToken)
            .getPayload();
    if (!"QR_CHECK_IN".equals(claims.get("purpose", String.class))) {
      throw new io.jsonwebtoken.JwtException("Invalid QR token purpose") {};
    }
    return java.util.UUID.fromString(claims.getSubject());
  }
}
