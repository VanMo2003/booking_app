<#
.SYNOPSIS
    Dựng phần còn lại sau khi đã có đủ công cụ: .env, database, dữ liệu mẫu.

.DESCRIPTION
    Chạy script này sau khi tools\check-env.ps1 báo đủ công cụ. Nó làm bốn việc:

      1. Tạo booking_app\.env (tự sinh JWT_SIGNER_KEY, hỏi mật khẩu MySQL)
      2. Tạo database booking_app_db nếu chưa có
      3. Khởi động backend một lần để Hibernate tạo bảng, rồi tắt đi
      4. Nạp db\seed_demo.sql

    Việc 3 là bắt buộc trước việc 4: file seed chỉ chèn dữ liệu, không tạo bảng.

    Script không ghi đè .env đang có trừ khi thêm -Force, và không bao giờ xoá
    database.

.PARAMETER MySqlUser
    Tài khoản MySQL, mặc định root.

.PARAMETER MySqlPassword
    Mật khẩu MySQL. Bỏ trống thì script hỏi (không hiện lên màn hình).

.PARAMETER GeminiApiKey
    Khoá Google Gemini để bật các tính năng AI. Bỏ trống cũng chạy được,
    chỉ là mọi lối vào AI trong app sẽ tự ẩn.

.PARAMETER Force
    Ghi đè .env đang có.

.PARAMETER SkipSeed
    Chỉ tạo .env và database, không khởi động backend và không nạp dữ liệu mẫu.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\tools\init-project.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\tools\init-project.ps1 -MySqlPassword 'matkhau' -GeminiApiKey 'AIza...'

.NOTES
    Mã thoát: 0 = xong, 1 = có bước thất bại.
#>

[CmdletBinding()]
param(
    [string]$MySqlUser = 'root',
    [string]$MySqlPassword,
    [string]$GeminiApiKey = '',
    [switch]$Force,
    [switch]$SkipSeed
)

$ErrorActionPreference = 'Continue'

$DB_NAME = 'booking_app_db'
$BE_PORT = 8080
$STATUS_URL = "http://localhost:$BE_PORT/booking-app/api/v1/ai/status"
$BOOT_TIMEOUT_SEC = 420

$BackendDir = Split-Path -Parent (Split-Path -Parent $PSCommandPath)
$EnvFile = Join-Path $BackendDir '.env'
$SeedFile = Join-Path $BackendDir 'db\seed_demo.sql'

function Write-Step($text) {
    Write-Host ''
    Write-Host "== $text" -ForegroundColor Cyan
}

function Write-Ok($text)   { Write-Host "  [OK]   $text" -ForegroundColor Green }
function Write-Warn2($text) { Write-Host "  [!]    $text" -ForegroundColor Yellow }
function Write-Err($text)  { Write-Host "  [LOI]  $text" -ForegroundColor Red }

function Get-MysqlExe {
    $cmd = Get-Command 'mysql' -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    foreach ($root in @("$env:ProgramFiles\MySQL", "${env:ProgramFiles(x86)}\MySQL")) {
        if (-not (Test-Path $root)) { continue }
        $found = Get-ChildItem -Path $root -Filter 'mysql.exe' -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return $null
}

function New-JwtKey {
    # HMAC-SHA512 can khoa tu 64 ky tu; lay 96 cho chac, chi dung chu va so
    # de khong vuong ky tu dac biet khi doc lai tu .env.
    $chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
    $bytes = New-Object byte[] 96
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $sb = New-Object System.Text.StringBuilder
    foreach ($b in $bytes) { [void]$sb.Append($chars[$b % $chars.Length]) }
    return $sb.ToString()
}

function Write-TextFileNoBom($path, $content) {
    # Set-Content -Encoding utf8 cua PowerShell 5.1 them BOM, lam hong ten bien
    # dau tien khi doc lai .env. Ghi tay de chac chan khong co BOM.
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($path, $content, $utf8)
}

function Import-DotEnv($path) {
    $map = @{}
    foreach ($line in (Get-Content $path | Where-Object { $_ -match '^[A-Z]' })) {
        $pair = $line -split '=', 2
        if ($pair.Count -eq 2) {
            $key = $pair[0].Trim()
            $value = $pair[1].Trim()
            $map[$key] = $value
            [Environment]::SetEnvironmentVariable($key, $value, 'Process')
        }
    }
    return $map
}

# ---------------------------------------------------------------------------
Write-Host ''
Write-Host '===========================================' -ForegroundColor Cyan
Write-Host ' booking_app - dung .env, database, du lieu' -ForegroundColor Cyan
Write-Host '===========================================' -ForegroundColor Cyan
Write-Host "Thu muc backend: $BackendDir" -ForegroundColor Gray

if (-not (Test-Path (Join-Path $BackendDir 'pom.xml'))) {
    Write-Err "Khong thay pom.xml trong $BackendDir. Chay script tu trong kho booking_app."
    exit 1
}

# ---------------------------------------------------------------------------
Write-Step '1. Tao .env'

if ((Test-Path $EnvFile) -and -not $Force) {
    Write-Ok ".env da co, giu nguyen (them -Force neu muon viet de)"
    $envMap = Import-DotEnv $EnvFile
    if ([string]::IsNullOrWhiteSpace($MySqlPassword)) {
        $MySqlPassword = $envMap['SPRING_DATASOURCE_PASSWORD']
    }
    if ($envMap.ContainsKey('SPRING_DATASOURCE_USERNAME') -and
        -not [string]::IsNullOrWhiteSpace($envMap['SPRING_DATASOURCE_USERNAME'])) {
        $MySqlUser = $envMap['SPRING_DATASOURCE_USERNAME']
    }
}
else {
    if ([string]::IsNullOrWhiteSpace($MySqlPassword)) {
        Write-Host "  Nhap mat khau MySQL cua tai khoan '$MySqlUser' (go xong bam Enter, man hinh khong hien gi):"
        $secure = Read-Host -AsSecureString
        $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
        try {
            $MySqlPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        }
        finally {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }

    $jwt = New-JwtKey
    $content = @"
# File nay do tools\init-project.ps1 tao. Khong bao gio dua len Git (.gitignore da chan).

# Co so du lieu
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/$DB_NAME`?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
SPRING_DATASOURCE_USERNAME=$MySqlUser
SPRING_DATASOURCE_PASSWORD=$MySqlPassword

# Khoa ky JWT, sinh ngau nhien 96 ky tu
JWT_SIGNER_KEY=$jwt

# Tinh nang AI. De trong thi app van chay, chi la moi loi vao AI tu an.
# Lay khoa mien phi tai https://aistudio.google.com > Get API key
GEMINI_API_KEY=$GeminiApiKey

# VNPay sandbox (de trong thi khong thanh toan online duoc)
VNPAY_TMN_CODE=
VNPAY_SECRET_KEY=
"@
    Write-TextFileNoBom $EnvFile $content
    Write-Ok "da tao $EnvFile"
    if ([string]::IsNullOrWhiteSpace($GeminiApiKey)) {
        Write-Warn2 'chua co GEMINI_API_KEY: cac man AI se tu an trong app'
    }
    $envMap = Import-DotEnv $EnvFile
}

# ---------------------------------------------------------------------------
Write-Step '2. Tao database'

$mysql = Get-MysqlExe
if (-not $mysql) {
    Write-Err 'Khong tim thay mysql.exe. Cai MySQL roi chay lai (xem SETUP.md muc 1.3).'
    exit 1
}
Write-Host "  dung: $mysql" -ForegroundColor Gray

$createSql = "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
$out = (& $mysql -u $MySqlUser "-p$MySqlPassword" -e $createSql) 2>&1 | Out-String
if ($LASTEXITCODE -ne 0) {
    if ($out -match 'Access denied') {
        Write-Err "Sai mat khau MySQL cho tai khoan '$MySqlUser'."
        Write-Err 'Sua SPRING_DATASOURCE_PASSWORD trong .env roi chay lai voi -Force.'
    }
    else {
        Write-Err ("mysql bao loi: " + $out.Trim())
    }
    exit 1
}
Write-Ok "database $DB_NAME da san sang"

if ($SkipSeed) {
    Write-Step 'Xong (bo qua phan du lieu mau vi co -SkipSeed)'
    Write-Host '  Chay backend: .\mvnw.cmd spring-boot:run' -ForegroundColor Gray
    exit 0
}

# ---------------------------------------------------------------------------
Write-Step '3. Khoi dong backend mot lan de tao bang'

# Hibernate tao bang luc khoi dong (ddl-auto=update). File seed chi chen du lieu,
# nen phai chay backend truoc, khong thi moi lenh INSERT deu bao thieu bang.
$alreadyUp = $false
try {
    $probe = Invoke-WebRequest -Uri $STATUS_URL -UseBasicParsing -TimeoutSec 5
    if ($probe.StatusCode -eq 200) { $alreadyUp = $true }
}
catch { }

$proc = $null
if ($alreadyUp) {
    Write-Ok "backend dang chay san o cong $BE_PORT, dung luon"
}
else {
    $logFile = Join-Path $env:TEMP 'booking_app_boot.log'
    if (Test-Path $logFile) { Remove-Item $logFile -Force }
    $mvnw = Join-Path $BackendDir 'mvnw.cmd'
    Write-Host '  dang khoi dong (lan dau Maven tai thu vien, co the vai phut)...' -ForegroundColor Gray
    $proc = Start-Process -FilePath $mvnw -ArgumentList 'spring-boot:run' -WorkingDirectory $BackendDir `
        -RedirectStandardOutput $logFile -RedirectStandardError "$logFile.err" -PassThru -WindowStyle Hidden

    $deadline = (Get-Date).AddSeconds($BOOT_TIMEOUT_SEC)
    $up = $false
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        if ($proc.HasExited) { break }
        try {
            $r = Invoke-WebRequest -Uri $STATUS_URL -UseBasicParsing -TimeoutSec 5
            if ($r.StatusCode -eq 200) { $up = $true; break }
        }
        catch { }
    }

    if (-not $up) {
        Write-Err "Backend khong len trong $BOOT_TIMEOUT_SEC giay."
        Write-Err "Xem log: $logFile"
        if (Test-Path $logFile) {
            Get-Content $logFile -Tail 25 | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
        }
        if ($proc -and -not $proc.HasExited) {
            & taskkill /PID $proc.Id /T /F 2>&1 | Out-Null
        }
        exit 1
    }
    Write-Ok 'backend da len, bang da duoc tao'
}

# ---------------------------------------------------------------------------
Write-Step '4. Nap du lieu mau'

if (-not (Test-Path $SeedFile)) {
    Write-Err "Khong thay $SeedFile"
}
else {
    # Dung lenh 'source' cua chinh mysql thay vi pipe tu PowerShell: pipe se doi
    # bang ma va lam hong tieng Viet trong file seed. Doi sang dau / vi 'source'
    # doc phan con lai cua dong lam ten file, dau \ de gay hieu nham.
    $seedPath = $SeedFile -replace '\\', '/'
    $seedOut = (& $mysql -u $MySqlUser "-p$MySqlPassword" $DB_NAME -e "source $seedPath") 2>&1 | Out-String
    if ($LASTEXITCODE -eq 0) {
        Write-Ok 'da nap db\seed_demo.sql'
        $check = (& $mysql -u $MySqlUser "-p$MySqlPassword" -N -B -D $DB_NAME `
            -e "SELECT COUNT(*) FROM hotels WHERE id BETWEEN 900 AND 999;") 2>&1 | Out-String
        if ($check -match '([1-9]\d*)') {
            Write-Ok "co $($Matches[1]) co so demo trong database"
        }
    }
    else {
        Write-Err ('nap du lieu mau that bai: ' + $seedOut.Trim())
    }
}

# ---------------------------------------------------------------------------
if ($proc -and -not $proc.HasExited) {
    Write-Step '5. Tat backend vua khoi dong'
    # mvnw.cmd sinh tien trinh java con, nen phai tat ca cay tien trinh
    & taskkill /PID $proc.Id /T /F 2>&1 | Out-Null
    Write-Ok 'da tat'
}

Write-Step 'Xong'
Write-Host '  Tu gio chay nhu sau:' -ForegroundColor Gray
Write-Host '    cd ' -NoNewline -ForegroundColor Gray; Write-Host $BackendDir -ForegroundColor Gray
Write-Host '    .\tools\run-backend.ps1          # hoac tu nap .env roi .\mvnw.cmd spring-boot:run' -ForegroundColor Gray
Write-Host '    cd ..\booking_app_demo; flutter run           # app Android' -ForegroundColor Gray
Write-Host '    cd ..\booking_app_demo; flutter run -d chrome # app web' -ForegroundColor Gray
Write-Host ''
Write-Host '  Dang nhap thu: manager_demo / manager123' -ForegroundColor Gray
exit 0
