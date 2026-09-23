# AI Commit CLI Installer for Windows
# Usage: irm https://raw.githubusercontent.com/kxng0109/ai-commit-cli/main/install.ps1 | iex
# Pinned version: $env:AI_COMMIT_VERSION = "v1.2.0"; irm ... | iex

param(
    [string]$Version = $env:AI_COMMIT_VERSION,
    [switch]$VerifyOnly
)

$ErrorActionPreference = "Stop"

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$REPO = "kxng0109/ai-commit-cli"
$INSTALL_DIR = "$env:LOCALAPPDATA\Programs\ai-commit"

Write-Host "==> Installing AI Commit CLI..." -ForegroundColor Cyan

if ([string]::IsNullOrWhiteSpace($Version)) {
    Write-Host "==> Fetching latest version..." -ForegroundColor Cyan
    try {
        $response = Invoke-RestMethod -Uri "https://api.github.com/repos/$REPO/releases/latest" -UseBasicParsing
        $Version = $response.tag_name
    } catch {
        Write-Host "Error: Failed to fetch latest version - $_" -ForegroundColor Red
        exit 1
    }
}
if ($Version -notmatch '^v\d+\.\d+\.\d+$') {
    Write-Host "Error: Refusing to use unexpected version value: $Version" -ForegroundColor Red
    exit 1
}
Write-Host "==> Version: $Version" -ForegroundColor Green

$ASSET = "ai-commit-windows-amd64.exe"
$URL = "https://github.com/$REPO/releases/download/$Version/$ASSET"
$SHA_URL = "$URL.sha256"
$TMP = Join-Path $env:TEMP ("ai-commit-" + [IO.Path]::GetRandomFileName() + ".exe")
$TMP_SHA = "$TMP.sha256"

Write-Host "==> Downloading..." -ForegroundColor Cyan
try {
    Invoke-WebRequest -Uri $URL -OutFile $TMP -UseBasicParsing
    Invoke-WebRequest -Uri $SHA_URL -OutFile $TMP_SHA -UseBasicParsing
} catch {
    Write-Host "Error: Download failed - $_" -ForegroundColor Red
    exit 1
}

Write-Host "==> Verifying checksum..." -ForegroundColor Cyan
$expected = ((Get-Content -LiteralPath $TMP_SHA -TotalCount 1).Split(' ')[0]).Trim().ToLower()
$actual = (Get-FileHash -LiteralPath $TMP -Algorithm SHA256).Hash.ToLower()
if ([string]::IsNullOrWhiteSpace($expected)) {
    Write-Host "Error: Empty checksum file" -ForegroundColor Red
    exit 1
}
if ($actual -ne $expected) {
    Write-Host "Error: Checksum mismatch (download may be tampered)" -ForegroundColor Red
    exit 1
}
Write-Host "==> Checksum OK" -ForegroundColor Green

if ($VerifyOnly) {
    Remove-Item -LiteralPath $TMP -ErrorAction SilentlyContinue
    Write-Host "==> Verify-only mode: checksum valid, nothing installed" -ForegroundColor Green
    exit 0
}
Remove-Item -LiteralPath $TMP_SHA -ErrorAction SilentlyContinue

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
