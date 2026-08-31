# run-all.ps1
# Starts all backend services for the attendance system in separate windows.
#   - Spring Boot (backend)  : port 8080
#   - FastAPI    (ai-service): port 8000
#   - React/Vite (frontend)  : port 5173
#
# Usage:
#   .\scripts\run-all.ps1           # start all services
#   .\scripts\run-all.ps1 -Only backend
#   .\scripts\run-all.ps1 -Only frontend
#   .\scripts\run-all.ps1 -Only ai
#
# Each service opens its own console window and writes its own log file under
# <repo>\scripts\logs\. Close a window or press its log to stop that service.

param(
    [ValidateSet("all", "backend", "ai", "frontend")]
    [string]$Only = "all",

    [switch]$NoFrontend
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$LogDir = Join-Path $PSScriptRoot "logs"
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Start-InWindow {
    param(
        [string]$Title,
        [string]$WorkDir,
        [string]$Command
    )
    $logFile = Join-Path $LogDir ($Title + ".log")
    $psCmd = "`$env:SPRING_PROFILES_ACTIVE='local'; Set-Location -LiteralPath '$WorkDir'; $Command *> '$logFile'"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $psCmd
    Write-Host "[START] $Title -> $logFile" -ForegroundColor Green
}

$doBackend  = ($Only -eq "all" -or $Only -eq "backend")
$doAi       = ($Only -eq "all" -or $Only -eq "ai")
$doFrontend = -not $NoFrontend -and ($Only -eq "all" -or $Only -eq "frontend")

if ($doBackend) {
    Start-InWindow -Title "backend" -WorkDir (Join-Path $Root "backend") `
        -Command ".\mvnw.cmd spring-boot:run"
}

if ($doAi) {
    Start-InWindow -Title "ai-service" -WorkDir (Join-Path $Root "ai-service") `
        -Command ".\venv\Scripts\python.exe -m app.main"
}

if ($doFrontend) {
    Start-InWindow -Title "frontend" -WorkDir (Join-Path $Root "frontend") `
        -Command "npm run dev"
}

Write-Host ""
Write-Host "All requested services launched. Logs: $LogDir" -ForegroundColor Yellow
