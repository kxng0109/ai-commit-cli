# AI Commit CLI Installer for Windows
# Usage: irm https://raw.githubusercontent.com/kxng0109/ai-commit-cli/main/install.ps1 | iex

$ErrorActionPreference = "Stop"

$REPO = "kxng0109/ai-commit-cli"
$INSTALL_DIR = "$env:LOCALAPPDATA\Programs\ai-commit"

Write-Host "==> Installing AI Commit CLI..." -ForegroundColor Cyan

Write-Host "==> Fetching latest version..." -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases/latest"
    $VERSION = $response.tag_name
    Write-Host "==> Latest version: $VERSION" -ForegroundColor Green
} catch {
    Write-Host "Error: Failed to fetch latest version - $_" -ForegroundColor Red
    exit 1
}

$URL = "https://github.com/$REPO/releases/download/$VERSION/ai-commit-windows-amd64.exe"
$TMP = "$env:TEMP\ai-commit-$PID.exe"

Write-Host "==> Downloading..." -ForegroundColor Cyan
try {
    Invoke-WebRequest -Uri $URL -OutFile $TMP
} catch {
    Write-Host "Error: Download failed - $_" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $INSTALL_DIR)) {
    New-Item -ItemType Directory -Path $INSTALL_DIR -Force | Out-Null
}

Write-Host "==> Installing to $INSTALL_DIR..." -ForegroundColor Cyan
Move-Item -Path $TMP -Destination "$INSTALL_DIR\ai-commit.exe" -Force

$currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($currentPath -notlike "*$INSTALL_DIR*") {
    Write-Host "==> Adding to PATH..." -ForegroundColor Cyan
    [Environment]::SetEnvironmentVariable("Path", "$currentPath;$INSTALL_DIR", "User")
    $env:Path += ";$INSTALL_DIR"
    Write-Host "    Note: Restart terminal to use globally" -ForegroundColor Yellow
} else {
    Write-Host "==> Already in PATH" -ForegroundColor Green
}

try {
    $version = & "$INSTALL_DIR\ai-commit.exe" --version 2>&1
    Write-Host "==> Installation complete: $version" -ForegroundColor Green
} catch {
    Write-Host "==> Installation complete!" -ForegroundColor Green
    Write-Host "    Installed to: $INSTALL_DIR\ai-commit.exe" -ForegroundColor Gray
}

Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Restart terminal (or run this in current session):"
Write-Host '     $env:Path += ";' -NoNewline
Write-Host $INSTALL_DIR -NoNewline -ForegroundColor Yellow
Write-Host '"'
Write-Host '  2. Configure API key: $env:OPENAI_API_KEY = "sk-..."'
Write-Host "  3. Use: cd your-repo; git add .; ai-commit"
Write-Host ""
Write-Host "Run 'ai-commit --help' for more info"