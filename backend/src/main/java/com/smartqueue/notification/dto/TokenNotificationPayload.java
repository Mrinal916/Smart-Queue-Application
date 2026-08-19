package com.smartqueue.notification.dto;

public record TokenNotificationPayload(
    String recipientEmail,
    String recipientName,
    String tokenNumber,
    String serviceName,
    String officeName,
    String departmentName,
    String queueDate,
    String appointmentTime,
    String status,
    String counterName,
    String message) {
  public static TokenNotificationPayload confirmation(
      String recipientEmail,
      String recipientName,
      String tokenNumber,
      String serviceName,
      String officeName,
      String departmentName,
      String queueDate,
      String appointmentTime) {
    return new TokenNotificationPayload(
        recipientEmail,
        recipientName,
        tokenNumber,
        serviceName,
        officeName,
        departmentName,
        queueDate,
        appointmentTime,
        "CONFIRMED",
        null,
        null);
  }

  public static TokenNotificationPayload statusUpdate(
      String recipientEmail,
      String recipientName,
      String tokenNumber,
      String serviceName,
      String status,
      String counterName,
      String message) {
    return new TokenNotificationPayload(
        recipientEmail,
        recipientName,
        tokenNumber,
        serviceName,
        null,
        null,
        null,
        null,
        status,
        counterName,
        message);
  }
}
