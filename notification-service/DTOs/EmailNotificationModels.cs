namespace SmartQueue.NotificationService.DTOs;

public class TwilioSettings
{
    public string AccountSid { get; set; } = string.Empty;
    public string AuthToken { get; set; } = string.Empty;
    public string ApiKey { get; set; } = string.Empty;
    public string FromEmail { get; set; } = "notifications@smartqueue.com";
    public string FromName { get; set; } = "SmartQueue Virtual Manager";
    public bool EnableMockMode { get; set; } = false;

    // SMTP Settings (e.g. Gmail: smtp.gmail.com, 587, SSL=true)
    public string SmtpHost { get; set; } = string.Empty;
    public int SmtpPort { get; set; } = 587;
    public string SmtpUsername { get; set; } = string.Empty;
    public string SmtpPassword { get; set; } = string.Empty;
    public bool EnableSsl { get; set; } = true;
}

public class WelcomeEmailRequest
{
    public string RecipientEmail { get; set; } = string.Empty;
    public string? RecipientName { get; set; }
}

public class TokenConfirmationRequest
{
    public string RecipientEmail { get; set; } = string.Empty;
    public string RecipientName { get; set; } = string.Empty;
    public string TokenNumber { get; set; } = string.Empty;
    public string ServiceName { get; set; } = string.Empty;
    public string OfficeName { get; set; } = string.Empty;
    public string DepartmentName { get; set; } = string.Empty;
    public string QueueDate { get; set; } = string.Empty;
    public string? AppointmentTime { get; set; }
    public string? QrCodePayload { get; set; }
}

public class TokenStatusUpdateRequest
{
    public string RecipientEmail { get; set; } = string.Empty;
    public string RecipientName { get; set; } = string.Empty;
    public string TokenNumber { get; set; } = string.Empty;
    public string ServiceName { get; set; } = string.Empty;
    public string Status { get; set; } = string.Empty; // CALLED, SERVED, CANCELLED, SKIPPED
    public string? CounterName { get; set; }
    public string? Message { get; set; }
}

public class GenericEmailRequest
{
    public string RecipientEmail { get; set; } = string.Empty;
    public string Subject { get; set; } = string.Empty;
    public string BodyHtml { get; set; } = string.Empty;
}

public class PasswordResetEmailRequest
{
    public string RecipientEmail { get; set; } = string.Empty;
    public string ResetLink { get; set; } = string.Empty;
}

public class NotificationResponse
{
    public bool Success { get; set; }
    public string Message { get; set; } = string.Empty;
    public string? MessageId { get; set; }
    public string Provider { get; set; } = "Twilio/SendGrid/SMTP";
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;
}
