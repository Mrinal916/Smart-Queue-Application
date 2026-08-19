# ==========================================
# SmartQueue Startup Script
# ==========================================

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "        Starting SmartQueue Project"
Write-Host "============================================" -ForegroundColor Cyan

# Project Location
$projectPath = $PSScriptRoot

# -----------------------------
# Start Backend
# -----------------------------
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    @"
cd '$projectPath'

# Load local environment settings. Values in .env override Spring Boot defaults.
`$envFile = Join-Path '$projectPath' '.env'
if (Test-Path `$envFile) {
    Get-Content `$envFile | ForEach-Object {
        `$line = `$_.Trim()
        if (`$line -and -not `$line.StartsWith('#')) {
            `$separator = `$line.IndexOf('=')
            if (`$separator -gt 0) {
                `$name = `$line.Substring(0, `$separator).Trim()
                `$value = `$line.Substring(`$separator + 1)
                if (`$name -match '^[A-Za-z_][A-Za-z0-9_]*$') {
                    Set-Item -Path ("Env:" + `$name) -Value `$value
                }
            }
        }
    }
    Write-Host 'Loaded local settings from .env' -ForegroundColor DarkGray
} else {
    Write-Warning 'No .env file found; using application configuration defaults.'
}

# Uncomment when testing on multiple PCs
# `$env:PASSWORD_RESET_URL='http://192.168.1.4:8080/reset-password'

mvn -pl backend spring-boot:run
"@
)

# -----------------------------
# Start Notification Service
# -----------------------------
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    @"
cd '$projectPath'
dotnet run --project notification-service/SmartQueue.NotificationService.csproj
"@
)

Start-Sleep -Seconds 8

Start-Process "http://localhost:8080"

Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "Backend and Notification Service Started"
Write-Host "============================================"
Write-Host ""

Write-Host "Admin Login"
Write-Host "Email    : admin@smartqueue.local"
Write-Host "Password : Admin@SmartQueue2026!"
Write-Host ""

Write-Host "Officer Login"
Write-Host "Email    : officer@smartqueue.local"
Write-Host "Password : Officer@SmartQueue2026!"
