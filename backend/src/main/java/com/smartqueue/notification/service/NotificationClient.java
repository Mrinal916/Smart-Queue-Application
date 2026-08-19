package com.smartqueue.notification.service;

import com.smartqueue.notification.dto.TokenNotificationPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class NotificationClient {

  private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
  private final RestClient restClient;

  public record WelcomeEmailPayload(String recipientEmail) {}

  public record PasswordResetEmailPayload(String recipientEmail, String resetLink) {}

  public NotificationClient(
      @Value("${notification.service.url:http://localhost:5050}") String serviceUrl) {
    this.restClient = RestClient.builder().baseUrl(serviceUrl).build();
  }

  @Async
  public void sendConfirmationEmail(TokenNotificationPayload payload) {
    try {
      log.info(
          "Sending confirmation email request to .NET notification service for token #{}",
          payload.tokenNumber());
      restClient
          .post()
          .uri("/api/notifications/email/send-confirmation")
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error(
          "Failed to send confirmation email via .NET notification service: {}", e.getMessage());
    }
  }

  @Async
  public void sendStatusUpdateEmail(TokenNotificationPayload payload) {
    try {
      log.info(
          "Sending status update email request to .NET notification service for token #{}",
          payload.tokenNumber());
      restClient
          .post()
          .uri("/api/notifications/email/send-status-update")
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error(
          "Failed to send status update email via .NET notification service: {}", e.getMessage());
    }
  }

  @Async
  public void sendWelcomeEmail(String recipientEmail) {
    try {
      log.info(
          "Sending welcome email request to .NET notification service for recipient {}",
          recipientEmail);
      restClient
          .post()
          .uri("/api/notifications/email/send-welcome")
          .body(new WelcomeEmailPayload(recipientEmail))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("Failed to send welcome email via .NET notification service: {}", e.getMessage());
    }
  }

  @Async
  public void sendPasswordResetEmail(String recipientEmail, String resetLink) {
    try {
      restClient
          .post()
          .uri("/api/notifications/email/send-password-reset")
          .body(new PasswordResetEmailPayload(recipientEmail, resetLink))
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error(
          "Failed to send password reset email via .NET notification service: {}", e.getMessage());
    }
  }
}
