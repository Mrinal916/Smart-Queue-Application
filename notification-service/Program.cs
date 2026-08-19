using SmartQueue.NotificationService.DTOs;
using SmartQueue.NotificationService.Services;

LoadDotEnv();

var builder = WebApplication.CreateBuilder(args);

// Configure Kestrel to run on port 5050
builder.WebHost.ConfigureKestrel(options =>
{
    options.ListenAnyIP(5050);
});

// Bind TwilioSettings from appsettings.json
builder.Services.Configure<TwilioSettings>(builder.Configuration.GetSection("TwilioSettings"));

// Register Services
builder.Services.AddScoped<IEmailService, TwilioEmailService>();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

// Configure CORS for local development
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowAll", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyHeader()
              .AllowAnyMethod();
    });
});

var app = builder.Build();

// Enable Swagger in Development and Production
app.UseSwagger();
app.UseSwaggerUI(c =>
{
    c.SwaggerEndpoint("/swagger/v1/swagger.json", "SmartQueue Notification Service API v1");
    c.RoutePrefix = "swagger";
});

app.UseCors("AllowAll");
app.UseAuthorization();
app.MapControllers();

app.Run();

static void LoadDotEnv()
{
    var directory = new DirectoryInfo(Directory.GetCurrentDirectory());

    while (directory is not null)
    {
        var envPath = Path.Combine(directory.FullName, ".env");
        if (File.Exists(envPath))
        {
            foreach (var rawLine in File.ReadLines(envPath))
            {
                var line = rawLine.Trim();
                if (string.IsNullOrWhiteSpace(line) || line.StartsWith('#'))
                {
                    continue;
                }

                if (line.StartsWith("export ", StringComparison.Ordinal))
                {
                    line = line[7..].TrimStart();
                }

                var separatorIndex = line.IndexOf('=');
                if (separatorIndex <= 0)
                {
                    continue;
                }

                var key = line[..separatorIndex].Trim();
                var value = line[(separatorIndex + 1)..].Trim();
                if (value.Length >= 2 && value[0] == value[^1] && (value[0] == '\"' || value[0] == '\''))
                {
                    value = value[1..^1];
                }

                if (string.IsNullOrEmpty(Environment.GetEnvironmentVariable(key)))
                {
                    Environment.SetEnvironmentVariable(key, value);
                }
            }

            return;
        }

        directory = directory.Parent;
    }
}
