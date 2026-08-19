using SmartQueue.NotificationService.DTOs;

namespace SmartQueue.NotificationService.Services;

public interface IEmailService
{
    Task<NotificationResponse> SendWelcomeEmailAsync(WelcomeEmailRequest request);
    Task<NotificationResponse> SendConfirmationEmailAsync(TokenConfirmationRequest request);
    Task<NotificationResponse> SendStatusUpdateEmailAsync(TokenStatusUpdateRequest request);
    Task<NotificationResponse> SendGenericEmailAsync(GenericEmailRequest request);
    Task<NotificationResponse> SendPasswordResetEmailAsync(PasswordResetEmailRequest request);
}
