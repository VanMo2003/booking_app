<#
.SYNOPSIS
    Nạp .env rồi chạy backend.

.DESCRIPTION
    Maven không tự đọc file .env, nên mỗi cửa sổ PowerShell mới đều phải nạp tay.
    Script này làm đúng việc đó rồi gọi mvnw.cmd spring-boot:run.

.PARAMETER Port
    Đổi cổng, mặc định lấy PORT trong .env hoặc 8080.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\tools\run-backend.ps1
#>

[CmdletBinding()]
param(
    [int]$Port
)

$BackendDir = Split-Path -Parent (Split-Path -Parent $PSCommandPath)
$EnvFile = Join-Path $BackendDir '.env'

if (-not (Test-Path $EnvFile)) {
    Write-Host "Khong thay $EnvFile." -ForegroundColor Red
    Write-Host 'Chay truoc: powershell -ExecutionPolicy Bypass -File .\tools\init-project.ps1' -ForegroundColor Yellow
    exit 1
}

$loaded = 0
foreach ($line in (Get-Content $EnvFile | Where-Object { $_ -match '^[A-Z]' })) {
    $pair = $line -split '=', 2
    if ($pair.Count -eq 2) {
        [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
        $loaded++
    }
}

if ($PSBoundParameters.ContainsKey('Port')) {
    [Environment]::SetEnvironmentVariable('PORT', "$Port", 'Process')
}

$shownPort = $env:PORT
if ([string]::IsNullOrWhiteSpace($shownPort)) { $shownPort = '8080' }

Write-Host "Da nap $loaded bien tu .env" -ForegroundColor Green
Write-Host "API se chay o http://localhost:$shownPort/booking-app/api/v1" -ForegroundColor Green
Write-Host ''

Set-Location $BackendDir
& (Join-Path $BackendDir 'mvnw.cmd') spring-boot:run
exit $LASTEXITCODE
