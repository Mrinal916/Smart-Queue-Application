using System.Net;
using System.Net.Mail;
using Microsoft.Extensions.Options;
using SendGrid;
using SendGrid.Helpers.Mail;
using SmartQueue.NotificationService.DTOs;

namespace SmartQueue.NotificationService.Services;

public class TwilioEmailService : IEmailService
{
    private readonly TwilioSettings _settings;
    private readonly ILogger<TwilioEmailService> _logger;

    public TwilioEmailService(IOptions<TwilioSettings> settings, ILogger<TwilioEmailService> logger)
    {
        _settings = settings.Value;
        _logger = logger;
    }

    public async Task<NotificationResponse> SendWelcomeEmailAsync(WelcomeEmailRequest request)
    {
        var recipientName = string.IsNullOrWhiteSpace(request.RecipientName)
            ? request.RecipientEmail.Split('@')[0]
            : request.RecipientName;

        var subject = "Welcome to SmartQueue";
        var bodyHtml = BuildWelcomeHtml(recipientName);

        return await SendEmailAsync(request.RecipientEmail, recipientName, subject, bodyHtml);
    }

    public async Task<NotificationResponse> SendConfirmationEmailAsync(TokenConfirmationRequest request)
    {
        var subject = $"Appointment confirmation: token #{request.TokenNumber} [{request.ServiceName}]";
        var bodyHtml = BuildConfirmationHtml(request);

        return await SendEmailAsync(request.RecipientEmail, request.RecipientName, subject, bodyHtml);
    }

    public async Task<NotificationResponse> SendStatusUpdateEmailAsync(TokenStatusUpdateRequest request)
    {
        var subject = request.Status switch
        {
            "CALLED" => $"Token #{request.TokenNumber} is called at {request.CounterName ?? "Service Desk"}",
            "SERVED" or "COMPLETED" => $"Visit completed: token #{request.TokenNumber} [{request.ServiceName}]",
            "CANCELLED" => $"Appointment cancelled: token #{request.TokenNumber}",
            "SKIPPED" => $"Token #{request.TokenNumber} skipped. Please contact reception",
            _ => $"SmartQueue update: token #{request.TokenNumber}"
        };

        var bodyHtml = BuildStatusUpdateHtml(request);

        return await SendEmailAsync(request.RecipientEmail, request.RecipientName, subject, bodyHtml);
    }

    public async Task<NotificationResponse> SendGenericEmailAsync(GenericEmailRequest request)
    {
        return await SendEmailAsync(request.RecipientEmail, request.RecipientEmail, request.Subject, request.BodyHtml);
    }

    public async Task<NotificationResponse> SendPasswordResetEmailAsync(PasswordResetEmailRequest request)
    {
        var recipientName = request.RecipientEmail.Split('@')[0];
        var subject = "Reset your SmartQueue password";
        var bodyHtml = BuildPasswordResetHtml(recipientName, request.ResetLink);
        return await SendEmailAsync(request.RecipientEmail, recipientName, subject, bodyHtml);
    }

    private async Task<NotificationResponse> SendEmailAsync(string toEmail, string toName, string subject, string htmlContent)
    {
        // Option 1: SendGrid API
        if (!string.IsNullOrWhiteSpace(_settings.ApiKey) && !_settings.EnableMockMode)
        {
            return await SendViaSendGridAsync(toEmail, toName, subject, htmlContent);
        }

        // Option 2: Standard SMTP (Gmail, Outlook, Custom SMTP)
        if (!string.IsNullOrWhiteSpace(_settings.SmtpHost) && !_settings.EnableMockMode)
        {
            return await SendViaSmtpAsync(toEmail, toName, subject, htmlContent);
        }

        // Option 3: Mock Mode (Log formatted email details to console)
        _logger.LogInformation("========================================================================");
        _logger.LogInformation("[SMARTQUEUE EMAIL DISPATCH SIMULATION]");
        _logger.LogInformation("To: {ToEmail} ({ToName})", toEmail, toName);
        _logger.LogInformation("From: {FromEmail} ({FromName})", _settings.FromEmail, _settings.FromName);
        _logger.LogInformation("Subject: {Subject}", subject);
        _logger.LogInformation("Status: DELIVERED (Simulated Dev Mode)");
        _logger.LogInformation("========================================================================");

        return new NotificationResponse
        {
            Success = true,
            Message = "Email generated and simulated successfully. Configure SendGrid or SMTP in appsettings.json for live inbox delivery.",
            MessageId = $"SMARTQUEUE-MSG-{Guid.NewGuid():N}",
            Provider = "SmartQueue Notification Engine (Mock)",
            Timestamp = DateTime.UtcNow
        };
    }

    private async Task<NotificationResponse> SendViaSendGridAsync(string toEmail, string toName, string subject, string htmlContent)
    {
        try
        {
            var client = new SendGridClient(_settings.ApiKey);
            var from = new SendGrid.Helpers.Mail.EmailAddress(_settings.FromEmail, _settings.FromName);
            var to = new SendGrid.Helpers.Mail.EmailAddress(toEmail, toName);
            var msg = MailHelper.CreateSingleEmail(from, to, subject, null, htmlContent);

            var response = await client.SendEmailAsync(msg);
            var isSuccess = (int)response.StatusCode >= 200 && (int)response.StatusCode < 300;

            string? messageId = null;
            if (response.Headers != null && response.Headers.TryGetValues("X-Message-Id", out var values))
            {
                messageId = values.FirstOrDefault();
            }

            _logger.LogInformation("SendGrid dispatch result: {StatusCode}, MessageId: {MessageId}", response.StatusCode, messageId);

            return new NotificationResponse
            {
                Success = isSuccess,
                Message = isSuccess ? "Email delivered successfully via SendGrid." : $"SendGrid status {(int)response.StatusCode}",
                MessageId = messageId ?? Guid.NewGuid().ToString("N"),
                Provider = "Twilio SendGrid",
                Timestamp = DateTime.UtcNow
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to dispatch SendGrid email to {ToEmail}", toEmail);
            return new NotificationResponse
            {
                Success = false,
                Message = $"Dispatch failed: {ex.Message}",
                Provider = "Twilio SendGrid",
                Timestamp = DateTime.UtcNow
            };
        }
    }

    private async Task<NotificationResponse> SendViaSmtpAsync(string toEmail, string toName, string subject, string htmlContent)
    {
        try
        {
            using var mailMessage = new MailMessage
            {
                From = new MailAddress(_settings.FromEmail, _settings.FromName),
                Subject = subject,
                Body = htmlContent,
                IsBodyHtml = true
            };
            mailMessage.To.Add(new MailAddress(toEmail, toName));

            using var smtpClient = new System.Net.Mail.SmtpClient(_settings.SmtpHost, _settings.SmtpPort)
            {
                EnableSsl = _settings.EnableSsl,
                Credentials = new NetworkCredential(_settings.SmtpUsername, _settings.SmtpPassword)
            };

            await smtpClient.SendMailAsync(mailMessage);
            _logger.LogInformation("SMTP email delivered to {ToEmail}", toEmail);

            return new NotificationResponse
            {
                Success = true,
                Message = $"Email delivered to {toEmail} via SMTP.",
                MessageId = $"SMTP-MSG-{Guid.NewGuid():N}",
                Provider = $"SMTP ({_settings.SmtpHost})",
                Timestamp = DateTime.UtcNow
            };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to dispatch SMTP email to {ToEmail}", toEmail);
            return new NotificationResponse
            {
                Success = false,
                Message = $"SMTP dispatch failed: {ex.Message}",
                Provider = "SMTP",
                Timestamp = DateTime.UtcNow
            };
        }
    }

    private static string BuildWelcomeHtml(string recipientName)
    {
        var safeRecipientName = WebUtility.HtmlEncode(recipientName);

        return $@"
<!DOCTYPE html>
<html>
<head>
  <meta charset='utf-8'>
  <style>
    body {{ font-family: Arial, sans-serif; background: #f6f8fb; color: #263238; margin: 0; padding: 24px; }}
    .card {{ max-width: 560px; margin: 0 auto; background: #ffffff; border: 1px solid #dde3ea; border-radius: 10px; overflow: hidden; }}
    .header {{ padding: 28px 32px 20px; border-bottom: 1px solid #e7ecf1; }}
    .brand {{ color: #155e75; font-size: 24px; font-weight: 700; margin: 0; }}
    .content {{ padding: 28px 32px; }}
    h2 {{ color: #1f2937; font-size: 20px; margin: 0 0 16px; }}
    p {{ color: #4b5563; font-size: 15px; line-height: 1.6; margin: 0 0 16px; }}
    .footer {{ padding: 20px 32px; background: #f8fafc; border-top: 1px solid #e7ecf1; color: #6b7280; font-size: 12px; line-height: 1.5; text-align: center; }}
  </style>
</head>
<body>
  <div class='card'>
    <div class='header'>
      <h1 class='brand'>SmartQueue</h1>
    </div>

    <div class='content'>
      <h2>Hi {safeRecipientName},</h2>
      <p>Your SmartQueue account is ready to use.</p>
      <p>You can now book an appointment, check your place in the queue, and receive updates about your visit.</p>
      <p>If you did not create this account, you can ignore this email.</p>
    </div>

    <div class='footer'>
      SmartQueue<br>
      You are receiving this email because an account was created with this address.
    </div>
  </div>
</body>
</html>";
    }

    private static string BuildPasswordResetHtml(string recipientName, string resetLink)
    {
        var safeRecipientName = WebUtility.HtmlEncode(recipientName);
        var safeResetLink = WebUtility.HtmlEncode(resetLink);

        return $@"
<!DOCTYPE html>
<html>
<head>
  <meta charset='utf-8'>
  <style>
    body {{ font-family: Arial, sans-serif; background: #f6f8fb; color: #263238; margin: 0; padding: 24px; }}
    .card {{ max-width: 560px; margin: 0 auto; background: #ffffff; border: 1px solid #dde3ea; border-radius: 10px; overflow: hidden; }}
    .header {{ padding: 28px 32px 20px; border-bottom: 1px solid #e7ecf1; }}
    .brand {{ color: #155e75; font-size: 24px; font-weight: 700; margin: 0; }}
    .content {{ padding: 28px 32px; }}
    h2 {{ color: #1f2937; font-size: 20px; margin: 0 0 16px; }}
    p {{ color: #4b5563; font-size: 15px; line-height: 1.6; margin: 0 0 16px; }}
    .button {{ display: inline-block; margin: 8px 0 20px; padding: 12px 18px; border-radius: 7px; background: #155e75; color: #ffffff; font-size: 15px; font-weight: 600; text-decoration: none; }}
    .footer {{ padding: 20px 32px; background: #f8fafc; border-top: 1px solid #e7ecf1; color: #6b7280; font-size: 12px; line-height: 1.5; text-align: center; }}
  </style>
</head>
<body>
  <div class='card'>
    <div class='header'><h1 class='brand'>SmartQueue</h1></div>
    <div class='content'>
      <h2>Reset your password</h2>
      <p>Hi {safeRecipientName},</p>
      <p>We received a request to reset the password for your SmartQueue account. This link expires in 30 minutes and can be used once.</p>
      <a class='button' href='{safeResetLink}'>Reset password</a>
      <p>If you did not request a password reset, you can ignore this email.</p>
    </div>
    <div class='footer'>SmartQueue<br>This email was sent because a password reset was requested for this account.</div>
  </div>
</body>
</html>";
    }

    private static string BuildConfirmationHtml(TokenConfirmationRequest req)
    {
        var apptTimeStr = string.IsNullOrWhiteSpace(req.AppointmentTime) ? "Standard Session" : req.AppointmentTime;
        return $@"
<!DOCTYPE html>
<html>
<head>
  <meta charset='utf-8'>
  <style>
    body {{ font-family: Arial, sans-serif; background: #f6f8fb; color: #263238; margin: 0; padding: 24px; }}
    .card {{ max-width: 560px; margin: 0 auto; background: #ffffff; border: 1px solid #dde3ea; border-radius: 10px; overflow: hidden; }}
    .header {{ padding: 28px 32px 20px; border-bottom: 1px solid #e7ecf1; }}
    .brand {{ color: #155e75; font-size: 24px; font-weight: 700; margin: 0; }}
    .content {{ padding: 28px 32px; }}
    h2 {{ color: #1f2937; font-size: 20px; margin: 0 0 14px; }}
    p {{ color: #4b5563; font-size: 15px; line-height: 1.6; }}
    .token-container {{ background: #f0f9ff; border: 1px solid #bae6fd; border-radius: 8px; padding: 18px; text-align: center; margin: 22px 0; }}
    .token-label {{ font-size: 13px; font-weight: 600; color: #4b5563; }}
    .token-number {{ font-size: 32px; font-weight: 700; color: #155e75; margin: 4px 0 0; }}
    .details-table {{ width: 100%; border-collapse: collapse; margin-top: 20px; }}
    .details-table td {{ padding: 10px 0; border-bottom: 1px solid #edf0f2; font-size: 14px; }}
    .label {{ color: #6b7280; width: 38%; }}
    .val {{ color: #263238; font-weight: 600; }}
    .notice-box {{ background: #fffbeb; border: 1px solid #fde68a; color: #654b12; padding: 14px; border-radius: 7px; font-size: 14px; margin-top: 22px; line-height: 1.5; }}
    .footer {{ padding: 20px 32px; background: #f8fafc; border-top: 1px solid #e7ecf1; font-size: 12px; color: #6b7280; text-align: center; line-height: 1.5; }}
  </style>
</head>
<body>
  <div class='card'>
    <div class='header'>
      <h1 class='brand'>SmartQueue</h1>
    </div>

    <div class='content'>
    <h2>Hi {req.RecipientName},</h2>
    <p>
      Your appointment has been booked for <strong>{req.OfficeName}</strong>.
    </p>

    <div class='token-container'>
      <div class='token-label'>Your queue token number</div>
      <div class='token-number'>#{req.TokenNumber}</div>
    </div>

    <table class='details-table'>
      <tr><td class='label'>Location:</td><td class='val'>{req.OfficeName}</td></tr>
      <tr><td class='label'>Department:</td><td class='val'>{req.DepartmentName}</td></tr>
      <tr><td class='label'>Service:</td><td class='val'>{req.ServiceName}</td></tr>
      <tr><td class='label'>Date:</td><td class='val'>{req.QueueDate}</td></tr>
      <tr><td class='label'>Time:</td><td class='val'>{apptTimeStr}</td></tr>
    </table>

    <div class='notice-box'>
      <strong>Visit details</strong><br>
      Please arrive a few minutes before your time slot. Keep this email or token number available when you check in.
    </div>
    </div>

    <div class='footer'>
      SmartQueue<br>
      This email confirms the appointment details you requested.
    </div>
  </div>
</body>
</html>";
    }

    private static string BuildStatusUpdateHtml(TokenStatusUpdateRequest req)
    {
        string statusTitle;
        string bannerBg;
        string bannerColor;

        switch (req.Status)
        {
            case "CALLED":
                statusTitle = "Your token has been called";
                bannerBg = "#dcfce7";
                bannerColor = "#15803d";
                break;
            case "SERVED":
            case "COMPLETED":
                statusTitle = "Visit completed";
                bannerBg = "#e0f2fe";
                bannerColor = "#0369a1";
                break;
            case "CANCELLED":
                statusTitle = "Appointment cancelled";
                bannerBg = "#fee2e2";
                bannerColor = "#b91c1c";
                break;
            case "SKIPPED":
                statusTitle = "Your token was skipped";
                bannerBg = "#fef3c7";
                bannerColor = "#b45309";
                break;
            default:
                statusTitle = "Appointment update";
                bannerBg = "#f1f5f9";
                bannerColor = "#334155";
                break;
        }

        var counterMsg = string.IsNullOrWhiteSpace(req.CounterName)
            ? ""
            : $"<p style='font-size: 1.2rem; font-weight: 800; color: #0284c7; margin: 12px 0;'>Please report to: {req.CounterName}</p>";

        var noteMsg = string.IsNullOrWhiteSpace(req.Message)
            ? ""
            : $"<div style='background: #f8fafc; padding: 14px; border-radius: 8px; border: 1px solid #e2e8f0; margin-top: 16px; font-size: 0.9rem;'>{req.Message}</div>";

        return $@"
<!DOCTYPE html>
<html>
<head>
  <meta charset='utf-8'>
  <style>
    body {{ font-family: Arial, sans-serif; background: #f6f8fb; color: #263238; margin: 0; padding: 24px; }}
    .card {{ max-width: 560px; margin: 0 auto; background: #ffffff; border: 1px solid #dde3ea; border-radius: 10px; overflow: hidden; }}
    .header {{ padding: 28px 32px 20px; border-bottom: 1px solid #e7ecf1; }}
    .brand {{ color: #155e75; font-size: 24px; font-weight: 700; margin: 0; }}
    .content {{ padding: 28px 32px; }}
    h2 {{ color: #1f2937; font-size: 20px; margin: 0 0 16px; }}
    .status-banner {{ background: {bannerBg}; color: {bannerColor}; font-weight: 600; padding: 14px; border-radius: 7px; font-size: 15px; text-align: center; margin: 0 0 20px; }}
    .token-display {{ font-size: 30px; font-weight: 700; color: #155e75; text-align: center; margin: 10px 0; }}
    .footer {{ padding: 20px 32px; background: #f8fafc; border-top: 1px solid #e7ecf1; font-size: 12px; color: #6b7280; text-align: center; line-height: 1.5; }}
  </style>
</head>
<body>
  <div class='card'>
    <div class='header'>
      <h1 class='brand'>SmartQueue</h1>
    </div>

    <div class='content'>
    <div class='status-banner'>{statusTitle}</div>
    <h2>Hi {req.RecipientName},</h2>
    
    <div class='token-display'>Token #{req.TokenNumber}</div>
    <p style='text-align: center; color: #475569; font-weight: 600; font-size: 1rem;'>Service: {req.ServiceName}</p>

    {counterMsg}
    {noteMsg}
    </div>

    <div class='footer'>
      SmartQueue<br>
      This email was sent to keep you updated about your queue.
    </div>
  </div>
</body>
</html>";
    }
}
