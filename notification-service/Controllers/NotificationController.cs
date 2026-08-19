using Microsoft.AspNetCore.Mvc;
using SmartQueue.NotificationService.DTOs;
using SmartQueue.NotificationService.Services;

namespace SmartQueue.NotificationService.Controllers;

[ApiController]
[Route("api/notifications")]
public class NotificationController : ControllerBase
{
    private readonly IEmailService _emailService;

    public NotificationController(IEmailService emailService)
    {
        _emailService = emailService;
    }

    [HttpGet("health")]
    public IActionResult HealthCheck()
    {
        return Ok(new
        {
            status = "UP",
            service = "SmartQueue .NET Email Notification Service (Twilio/SendGrid)",
            timestamp = DateTime.UtcNow
        });
    }

    [HttpPost("email/send-welcome")]
    public async Task<IActionResult> SendWelcome([FromBody] WelcomeEmailRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.RecipientEmail))
        {
            return BadRequest(new { error = "RecipientEmail is a required field." });
        }

        var result = await _emailService.SendWelcomeEmailAsync(request);
        return result.Success ? Ok(result) : StatusCode(500, result);
    }

    [HttpPost("email/send-confirmation")]
    public async Task<IActionResult> SendConfirmation([FromBody] TokenConfirmationRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.RecipientEmail) || string.IsNullOrWhiteSpace(request.TokenNumber))
        {
            return BadRequest(new { error = "RecipientEmail and TokenNumber are required fields." });
        }

        if (string.IsNullOrWhiteSpace(request.RecipientName))
        {
            request.RecipientName = request.RecipientEmail.Split('@')[0];
        }

        var result = await _emailService.SendConfirmationEmailAsync(request);
        return result.Success ? Ok(result) : StatusCode(500, result);
    }

    [HttpPost("email/send-status-update")]
    public async Task<IActionResult> SendStatusUpdate([FromBody] TokenStatusUpdateRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.RecipientEmail) || string.IsNullOrWhiteSpace(request.TokenNumber))
        {
            return BadRequest(new { error = "RecipientEmail and TokenNumber are required fields." });
        }

        if (string.IsNullOrWhiteSpace(request.RecipientName))
        {
            request.RecipientName = request.RecipientEmail.Split('@')[0];
        }

        var result = await _emailService.SendStatusUpdateEmailAsync(request);
        return result.Success ? Ok(result) : StatusCode(500, result);
    }

    [HttpPost("email/send-generic")]
    public async Task<IActionResult> SendGeneric([FromBody] GenericEmailRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.RecipientEmail) || string.IsNullOrWhiteSpace(request.Subject))
        {
            return BadRequest(new { error = "RecipientEmail and Subject are required fields." });
        }

        var result = await _emailService.SendGenericEmailAsync(request);
        return result.Success ? Ok(result) : StatusCode(500, result);
    }

    [HttpPost("email/send-password-reset")]
    public async Task<IActionResult> SendPasswordReset([FromBody] PasswordResetEmailRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.RecipientEmail) || string.IsNullOrWhiteSpace(request.ResetLink))
        {
            return BadRequest(new { error = "RecipientEmail and ResetLink are required fields." });
        }

        var result = await _emailService.SendPasswordResetEmailAsync(request);
        return result.Success ? Ok(result) : StatusCode(500, result);
    }
}
