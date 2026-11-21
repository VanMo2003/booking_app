<#
.SYNOPSIS
    Tạo Secret booking-app-secrets trong namespace booking-dev từ file .env.

.DESCRIPTION
    Khoá bí mật KHÔNG nằm trong Git, nên Argo CD không quản Secret này - nó chỉ quản
    những gì có trong k8s/. Chạy script một lần trước khi Argo CD đồng bộ lần đầu, và
    chạy lại mỗi khi đổi khoá (thêm -Force để ghi đè).

    Muốn đưa Secret vào Git một cách an toàn thì dùng Sealed Secrets hoặc SOPS; với đồ án
    thì tạo tay như thế này là đủ và trung thực hơn là commit khoá ra repo.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\k8s\create-secret.ps1
#>

[CmdletBinding()]
param(
    [string]$Namespace = 'booking-dev',
    [string]$EnvFile,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
if (-not $EnvFile) { $EnvFile = Join-Path $Root '.env' }
if (-not (Test-Path $EnvFile)) { throw "Khong thay $EnvFile" }

# Đọc .env. Giá trị không in ra màn hình ở bất kỳ đâu trong script này.
$env2 = @{}
foreach ($line in Get-Content $EnvFile) {
    if ($line -match '^\s*#') { continue }
    if ($line -match '^\s*([A-Z0-9_]+)\s*=\s*(.*)$') { $env2[$Matches[1]] = $Matches[2].Trim() }
}

# Những khoá app thật sự cần. DB trong cụm là MySQL riêng của cụm, nên user/mật khẩu
# lấy giá trị cố định chứ không lấy từ .env (cái đó là của MySQL trên máy).
$literals = [ordered]@{
    SPRING_DATASOURCE_USERNAME = 'booking_user'
    SPRING_DATASOURCE_PASSWORD = 'booking_password'
    MYSQL_ROOT_PASSWORD        = 'root_password'
}

# Những khoá chép từ .env sang.
$fromEnv = @(
    'JWT_SIGNER_KEY',
    'GEMINI_API_KEY', 'ANTHROPIC_API_KEY', 'AI_PROVIDER', 'AI_API_KEY', 'AI_BASE_URL',
    'AI_ASSISTANT_MODEL', 'AI_FAST_MODEL',
    'VNPAY_TMN_CODE', 'VNPAY_SECRET_KEY',
    'MAIL_USERNAME', 'MAIL_PASSWORD', 'MAIL_FROM'
)

if ([string]::IsNullOrWhiteSpace($env2['JWT_SIGNER_KEY'])) {
    throw 'Thieu JWT_SIGNER_KEY trong .env'
}

$args2 = @('create', 'secret', 'generic', 'booking-app-secrets', '-n', $Namespace)
foreach ($k in $literals.Keys) { $args2 += "--from-literal=$k=$($literals[$k])" }
foreach ($k in $fromEnv) {
    $v = $env2[$k]
    if ($null -eq $v) { $v = '' }
    $args2 += "--from-literal=$k=$v"
}
# Dựng YAML rồi apply, để chạy lại cũng không lỗi "đã tồn tại".
$args2 += @('--dry-run=client', '-o', 'yaml')

kubectl create namespace $Namespace 2>$null | Out-Null

if (-not $Force) {
    $existing = kubectl get secret booking-app-secrets -n $Namespace --ignore-not-found -o name 2>$null
    if ($existing) {
        Write-Host "Secret da co trong namespace $Namespace. Them -Force de ghi de." -ForegroundColor Yellow
        exit 0
    }
}

$yaml = & kubectl @args2
if ($LASTEXITCODE -ne 0) { throw 'kubectl create secret that bai' }
$yaml | kubectl apply -n $Namespace -f - | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'kubectl apply that bai' }

$count = $literals.Count + $fromEnv.Count
Write-Host "Da tao secret booking-app-secrets ($count khoa) trong namespace $Namespace" -ForegroundColor Green
$missing = $fromEnv | Where-Object { [string]::IsNullOrWhiteSpace($env2[$_]) }
if ($missing) {
    Write-Host ("  De trong (tinh nang tuong ung se tat): " + ($missing -join ', ')) -ForegroundColor DarkGray
}
