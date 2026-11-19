<#
.SYNOPSIS
    Kiểm tra (và cài giúp) những thứ cần có để chạy booking_app.

.DESCRIPTION
    Mặc định script CHỈ KIỂM TRA và in ra bảng kết quả, không đụng gì vào máy.
    Thêm -Install thì nó cài những thứ còn thiếu bằng winget (Flutter tải zip vì
    không có gói winget).

    Kiểm tra hai nhóm:
      1. Công cụ:  Git, JDK 21, MySQL, Flutter, Android Studio, Chrome
      2. Dự án:    đã clone mã chưa, có .env chưa, MySQL có chạy không,
                   database đã tạo chưa, backend có đang chạy không

.PARAMETER Install
    Cài những thứ còn thiếu. Nên mở PowerShell bằng "Run as administrator".

.PARAMETER Yes
    Không hỏi lại trước mỗi lần cài.

.PARAMETER ProjectRoot
    Thư mục chứa hai kho mã. Mặc định là thư mục cha của kho booking_app.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\tools\check-env.ps1
    Chỉ xem còn thiếu gì. Máy mới cài Windows chặn chạy script, nên gọi kèm
    -ExecutionPolicy Bypass như trên thay vì gọi thẳng .\tools\check-env.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\tools\check-env.ps1 -Install -Yes
    Cài hết những thứ còn thiếu, không hỏi lại. Nên mở PowerShell bằng
    "Run as administrator" trước.

.NOTES
    Mã thoát: 0 = đủ hết, 1 = còn thiếu thứ bắt buộc.
    Script không bao giờ gỡ hay hạ cấp phần mềm đã có.
#>

[CmdletBinding()]
param(
    [switch]$Install,
    [switch]$Yes,
    [string]$ProjectRoot
)

$ErrorActionPreference = 'Continue'

# Phiên bản tối thiểu dự án cần
$JAVA_MAJOR = 21
$FLUTTER_MIN = [version]'3.47.0'
$FLUTTER_HOME = 'C:\src\flutter'
$DB_NAME = 'booking_app_db'
$BE_PORT = 8080

$script:Results = @()
$script:MysqlExe = $null
$script:MissingRequired = 0

# ---------------------------------------------------------------------------
# Tiện ích in ấn
# ---------------------------------------------------------------------------

function Write-Head($text) {
    Write-Host ''
    Write-Host "== $text" -ForegroundColor Cyan
}

function Add-Result($name, $state, $detail, $fix) {
    $script:Results += [pscustomobject]@{
        Muc    = $name
        TinhTrang = $state
        ChiTiet = $detail
        Sua    = $fix
    }
    switch ($state) {
        'OK'      { Write-Host ("  [OK]      {0,-18} {1}" -f $name, $detail) -ForegroundColor Green }
        'THIEU'   { Write-Host ("  [THIEU]   {0,-18} {1}" -f $name, $detail) -ForegroundColor Red }
        'SAI'     { Write-Host ("  [SAI]     {0,-18} {1}" -f $name, $detail) -ForegroundColor Red }
        'CANXEM'  { Write-Host ("  [CAN XEM] {0,-18} {1}" -f $name, $detail) -ForegroundColor Yellow }
        default   { Write-Host ("  [{0}] {1,-18} {2}" -f $state, $name, $detail) }
    }
}

function Get-MysqlExe {
    # mysql.exe thuong khong duoc them vao PATH luc cai, nen tim them o cho mac dinh
    # truoc khi ket luan la thieu.
    if ($script:MysqlExe) { return $script:MysqlExe }
    $onPath = Test-Command 'mysql'
    if ($onPath) {
        $script:MysqlExe = $onPath
        return $onPath
    }
    $roots = @(
        "$env:ProgramFiles\MySQL",
        "${env:ProgramFiles(x86)}\MySQL"
    )
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        $found = Get-ChildItem -Path $root -Filter 'mysql.exe' -Recurse -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1
        if ($found) {
            $script:MysqlExe = $found.FullName
            return $script:MysqlExe
        }
    }
    return $null
}

function Test-Command($name) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($null -eq $cmd) { return $null }
    return $cmd.Source
}

# ---------------------------------------------------------------------------
# Cài đặt
# ---------------------------------------------------------------------------

function Test-Admin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($id)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Confirm-Install($what) {
    if (-not $Install) { return $false }
    if ($Yes) { return $true }
    $answer = Read-Host "  Cai $what bay gio? (y/N)"
    return ($answer -eq 'y' -or $answer -eq 'Y')
}

function Install-WingetPackage($id, $label) {
    if (-not (Confirm-Install $label)) {
        Write-Host "  -> bo qua, chua cai $label" -ForegroundColor Yellow
        return $false
    }
    if (-not (Test-Command 'winget')) {
        Write-Host "  -> khong co winget. Cai 'App Installer' tu Microsoft Store roi chay lai." -ForegroundColor Red
        return $false
    }
    Write-Host "  -> dang cai $label ($id)..." -ForegroundColor Cyan
    winget install --id $id --exact --silent --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  -> da cai xong $label. Mo PowerShell MOI de thay lenh moi." -ForegroundColor Green
        return $true
    }
    Write-Host "  -> winget tra ve ma $LASTEXITCODE. Thu chay lai bang quyen Administrator." -ForegroundColor Red
    return $false
}

function Install-Flutter {
    # Flutter khong co goi winget, nen tai zip chinh chu ve C:\src\flutter.
    if (-not (Confirm-Install 'Flutter SDK')) {
        Write-Host '  -> bo qua, chua cai Flutter' -ForegroundColor Yellow
        return $false
    }
    if (Test-Path (Join-Path $FLUTTER_HOME 'bin\flutter.bat')) {
        Write-Host "  -> da co san o $FLUTTER_HOME, chi can them vao PATH" -ForegroundColor Yellow
    }
    else {
        if (-not (Test-Command 'git')) {
            Write-Host '  -> can Git truoc da. Chay lai script sau khi cai Git.' -ForegroundColor Red
            return $false
        }
        $parent = Split-Path $FLUTTER_HOME -Parent
        if (-not (Test-Path $parent)) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
        Write-Host "  -> dang tai Flutter (stable) ve $FLUTTER_HOME, mat vai phut..." -ForegroundColor Cyan
        git clone --depth 1 -b stable https://github.com/flutter/flutter.git $FLUTTER_HOME
        if ($LASTEXITCODE -ne 0) {
            Write-Host '  -> tai that bai. Tai tay tai https://docs.flutter.dev/get-started/install/windows' -ForegroundColor Red
            return $false
        }
    }

    $bin = Join-Path $FLUTTER_HOME 'bin'
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    if ($userPath -notlike "*$bin*") {
        [Environment]::SetEnvironmentVariable('Path', "$userPath;$bin", 'User')
        Write-Host "  -> da them $bin vao PATH cua nguoi dung" -ForegroundColor Green
    }
    $env:Path = "$env:Path;$bin"
    Write-Host '  -> da cai Flutter. Mo PowerShell MOI roi chay: flutter doctor' -ForegroundColor Green
    return $true
}

# ---------------------------------------------------------------------------
# 1. Công cụ
# ---------------------------------------------------------------------------

function Check-Winget {
    if (Test-Command 'winget') {
        $v = (winget --version) 2>$null
        Add-Result 'winget' 'OK' "$v" ''
        return $true
    }
    Add-Result 'winget' 'THIEU' 'khong co, khong tu cai duoc gi' 'Cai "App Installer" tu Microsoft Store'
    return $false
}

function Check-Git {
    if (Test-Command 'git') {
        $v = (git --version) 2>$null
        Add-Result 'Git' 'OK' $v ''
        return
    }
    $script:MissingRequired++
    Add-Result 'Git' 'THIEU' 'chua cai' 'winget install --id Git.Git'
    Install-WingetPackage 'Git.Git' 'Git' | Out-Null
}

function Check-Java {
    $javaPath = Test-Command 'java'
    $major = 0
    if ($javaPath) {
        # java -version in ra stderr, nen phai gom ca hai luong
        $raw = (& java -version 2>&1 | Out-String)
        if ($raw -match '"(\d+)[\."]') { $major = [int]$Matches[1] }
    }

    if ($major -eq $JAVA_MAJOR) {
        Add-Result 'JDK 21' 'OK' "java $major ($javaPath)" ''
    }
    elseif ($major -gt 0) {
        $script:MissingRequired++
        Add-Result 'JDK 21' 'SAI' "dang la java $major, du an can $JAVA_MAJOR" 'Cai Temurin 21 roi tro JAVA_HOME vao do'
        Install-WingetPackage 'EclipseAdoptium.Temurin.21.JDK' 'JDK 21' | Out-Null
    }
    else {
        $script:MissingRequired++
        Add-Result 'JDK 21' 'THIEU' 'chua cai' 'winget install --id EclipseAdoptium.Temurin.21.JDK'
        Install-WingetPackage 'EclipseAdoptium.Temurin.21.JDK' 'JDK 21' | Out-Null
    }

    # JAVA_HOME phai tro vao JDK 21, neu khong Maven se dung ban khac.
    # Dat ten bien la $javaHome chu khong phai $home: $HOME la bien co san cua
    # PowerShell va chi doc, gan vao do se that bai am tham roi bao sai.
    $javaHome = $env:JAVA_HOME
    if ([string]::IsNullOrWhiteSpace($javaHome)) {
        Add-Result 'JAVA_HOME' 'CANXEM' 'chua dat' 'Dat JAVA_HOME tro vao thu muc JDK 21'
    }
    elseif (-not (Test-Path (Join-Path $javaHome 'bin\java.exe'))) {
        Add-Result 'JAVA_HOME' 'SAI' "$javaHome khong phai thu muc JDK" 'Tro JAVA_HOME vao thu muc JDK 21'
    }
    else {
        $hv = (& (Join-Path $javaHome 'bin\java.exe') -version 2>&1 | Out-String)
        $hm = 0
        if ($hv -match '"(\d+)[\."]') { $hm = [int]$Matches[1] }
        if ($hm -eq $JAVA_MAJOR) {
            Add-Result 'JAVA_HOME' 'OK' $javaHome ''
        }
        else {
            Add-Result 'JAVA_HOME' 'SAI' "$javaHome dang la java $hm" "Tro JAVA_HOME vao JDK $JAVA_MAJOR"
        }
    }
}

function Check-MySql {
    $service = Get-Service -Name 'MySQL*' -ErrorAction SilentlyContinue | Select-Object -First 1
    $client = Get-MysqlExe

    if ($null -eq $service -and -not $client) {
        $script:MissingRequired++
        Add-Result 'MySQL' 'THIEU' 'chua cai' 'winget install --id Oracle.MySQL'
        Install-WingetPackage 'Oracle.MySQL' 'MySQL Server' | Out-Null
        return
    }

    if ($null -ne $service) {
        if ($service.Status -eq 'Running') {
            Add-Result 'MySQL' 'OK' "$($service.Name) dang chay" ''
        }
        else {
            Add-Result 'MySQL' 'CANXEM' "$($service.Name) dang dung" "Start-Service $($service.Name)"
        }
    }
    else {
        Add-Result 'MySQL' 'CANXEM' 'co lenh mysql nhung khong thay service' 'Kiem tra MySQL Server da cai chua'
    }

    if (-not $client) {
        Add-Result 'mysql CLI' 'CANXEM' 'khong tim thay mysql.exe' 'Them ...\MySQL Server 8.x\bin vao PATH'
    }
    elseif (-not (Test-Command 'mysql')) {
        Add-Result 'mysql CLI' 'OK' "$client (ngoai PATH)" 'Nen them thu muc nay vao PATH cho tien'
    }
    else {
        Add-Result 'mysql CLI' 'OK' $client ''
    }
}

function Check-Flutter {
    if (-not (Test-Command 'flutter')) {
        $script:MissingRequired++
        Add-Result 'Flutter' 'THIEU' 'chua cai' "Tai ve $FLUTTER_HOME roi them bin vao PATH"
        Install-Flutter | Out-Null
        return
    }
    $raw = (& flutter --version 2>&1 | Out-String)
    if ($raw -match 'Flutter\s+(\d+\.\d+\.\d+)') {
        $v = [version]$Matches[1]
        if ($v -ge $FLUTTER_MIN) {
            Add-Result 'Flutter' 'OK' "$v" ''
        }
        else {
            $script:MissingRequired++
            Add-Result 'Flutter' 'SAI' "$v, du an can tu $FLUTTER_MIN" 'flutter upgrade'
        }
    }
    else {
        Add-Result 'Flutter' 'CANXEM' 'khong doc duoc phien ban' 'Chay: flutter --version'
    }
}

function Check-AndroidStudio {
    $sdk = $env:ANDROID_HOME
    if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = $env:ANDROID_SDK_ROOT }
    if ([string]::IsNullOrWhiteSpace($sdk)) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }

    if (Test-Path $sdk) {
        Add-Result 'Android SDK' 'OK' $sdk ''
    }
    else {
        # Khong bat buoc: thieu thi chi mat phan chay tren Android, web va API van chay
        Add-Result 'Android SDK' 'CANXEM' 'chua thay SDK, se khong chay duoc tren Android' 'Cai Android Studio roi mo mot lan'
        Install-WingetPackage 'Google.AndroidStudio' 'Android Studio' | Out-Null
    }
}

function Check-Chrome {
    $paths = @(
        "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
        "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
        "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
    )
    foreach ($p in $paths) {
        if (Test-Path $p) {
            Add-Result 'Chrome' 'OK' $p ''
            return
        }
    }
    Add-Result 'Chrome' 'CANXEM' 'chua thay, se khong chay duoc ban web' 'winget install --id Google.Chrome'
    Install-WingetPackage 'Google.Chrome' 'Google Chrome' | Out-Null
}

# ---------------------------------------------------------------------------
# 2. Dự án
# ---------------------------------------------------------------------------

function Resolve-ProjectRoot {
    if (-not [string]::IsNullOrWhiteSpace($ProjectRoot)) { return $ProjectRoot }
    # Script nam trong booking_app\tools, nen thu muc cha cua booking_app la goc
    $here = Split-Path -Parent $PSCommandPath
    return (Split-Path -Parent (Split-Path -Parent $here))
}

function Check-Repos($root) {
    $be = Join-Path $root 'booking_app'
    $fe = Join-Path $root 'booking_app_demo'

    if (Test-Path (Join-Path $be 'pom.xml')) {
        $branch = (git -C $be branch --show-current) 2>$null
        if ($branch -eq 'develop') {
            Add-Result 'Ma backend' 'OK' "$be (develop)" ''
        }
        else {
            Add-Result 'Ma backend' 'CANXEM' "$be dang o nhanh '$branch'" 'git checkout develop'
        }
    }
    else {
        $script:MissingRequired++
        Add-Result 'Ma backend' 'THIEU' "khong thay $be" 'git clone -b develop https://github.com/VanMo2003/booking_app.git'
    }

    if (Test-Path (Join-Path $fe 'pubspec.yaml')) {
        $branch = (git -C $fe branch --show-current) 2>$null
        if ($branch -eq 'develop') {
            Add-Result 'Ma app' 'OK' "$fe (develop)" ''
        }
        else {
            Add-Result 'Ma app' 'CANXEM' "$fe dang o nhanh '$branch'" 'git checkout develop'
        }
        if (Test-Path (Join-Path $fe 'lib\core\navigation\app_router.gr.dart')) {
            Add-Result 'Ma sinh tu dong' 'OK' 'da co app_router.gr.dart' ''
        }
        else {
            Add-Result 'Ma sinh tu dong' 'CANXEM' 'chua sinh ma' 'dart run build_runner build --delete-conflicting-outputs'
        }
    }
    else {
        $script:MissingRequired++
        Add-Result 'Ma app' 'THIEU' "khong thay $fe" 'git clone -b develop https://github.com/VanMo2003/booking_app_demo.git'
    }

    return $be
}

function Check-EnvFile($backendDir) {
    $envFile = Join-Path $backendDir '.env'
    if (-not (Test-Path $envFile)) {
        $script:MissingRequired++
        Add-Result '.env' 'THIEU' "chua co $envFile" 'Xem phan 4 trong SETUP.md'
        return
    }
    $lines = Get-Content $envFile | Where-Object { $_ -match '^[A-Z]' }
    $map = @{}
    foreach ($line in $lines) {
        $pair = $line -split '=', 2
        if ($pair.Count -eq 2) { $map[$pair[0].Trim()] = $pair[1].Trim() }
    }

    $missing = @()
    foreach ($key in @('SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'JWT_SIGNER_KEY')) {
        if (-not $map.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($map[$key])) { $missing += $key }
    }
    if ($missing.Count -gt 0) {
        $script:MissingRequired++
        Add-Result '.env' 'SAI' ("thieu gia tri: " + ($missing -join ', ')) 'Xem phan 4 trong SETUP.md'
        return
    }

    # JWT_SIGNER_KEY ngan hon 64 ky tu se lam backend sap ngay khi dang nhap (HMAC-SHA512)
    if ($map['JWT_SIGNER_KEY'].Length -lt 64) {
        $script:MissingRequired++
        Add-Result '.env' 'SAI' "JWT_SIGNER_KEY chi dai $($map['JWT_SIGNER_KEY'].Length) ky tu, can tu 64" 'Dat mot chuoi ngau nhien dai hon'
        return
    }

    $ai = ''
    if ($map.ContainsKey('GEMINI_API_KEY') -and -not [string]::IsNullOrWhiteSpace($map['GEMINI_API_KEY'])) {
        $ai = ', co khoa AI'
    }
    else {
        $ai = ', chua co khoa AI (cac man AI se tu an)'
    }
    Add-Result '.env' 'OK' ("day du" + $ai) ''
}

function Check-Database($backendDir) {
    $mysql = Get-MysqlExe
    if (-not $mysql) {
        Add-Result 'Database' 'CANXEM' 'khong tim thay mysql.exe de kiem tra' 'Cai MySQL hoac them vao PATH'
        return
    }
    $envFile = Join-Path $backendDir '.env'
    if (-not (Test-Path $envFile)) {
        # Khong doan mat khau root: noi ro vi sao chua kiem tra duoc, dung im lang bo qua.
        Add-Result 'Database' 'CANXEM' 'chua kiem tra duoc vi chua co .env' 'Tao .env roi chay lai script'
        return
    }

    $map = @{}
    foreach ($line in (Get-Content $envFile | Where-Object { $_ -match '^[A-Z]' })) {
        $pair = $line -split '=', 2
        if ($pair.Count -eq 2) { $map[$pair[0].Trim()] = $pair[1].Trim() }
    }
    $user = $map['SPRING_DATASOURCE_USERNAME']
    $pass = $map['SPRING_DATASOURCE_PASSWORD']
    if ([string]::IsNullOrWhiteSpace($user)) {
        Add-Result 'Database' 'CANXEM' 'chua co SPRING_DATASOURCE_USERNAME trong .env' 'Xem phan 4 trong SETUP.md'
        return
    }

    $query = "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$DB_NAME';"
    $out = (& $mysql -u $user "-p$pass" -N -B -e $query) 2>&1 | Out-String
    if ($out -match '^\s*1\s*$') {
        $tables = (& $mysql -u $user "-p$pass" -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';") 2>&1 | Out-String
        $n = 0
        if ($tables -match '(\d+)') { $n = [int]$Matches[1] }
        if ($n -gt 0) {
            Add-Result 'Database' 'OK' "$DB_NAME co $n bang" ''
            $demo = (& $mysql -u $user "-p$pass" -N -B -D $DB_NAME -e "SELECT COUNT(*) FROM hotels WHERE id BETWEEN 900 AND 999;") 2>&1 | Out-String
            if ($demo -match '([1-9]\d*)') {
                Add-Result 'Du lieu mau' 'OK' 'da nap seed_demo.sql' ''
            }
            else {
                Add-Result 'Du lieu mau' 'CANXEM' 'chua nap' "mysql -u $user -p $DB_NAME < db\seed_demo.sql"
            }
        }
        else {
            Add-Result 'Database' 'CANXEM' "$DB_NAME con rong" 'Chay backend mot lan de Hibernate tao bang'
        }
    }
    elseif ($out -match 'Access denied') {
        $script:MissingRequired++
        Add-Result 'Database' 'SAI' 'sai tai khoan hoac mat khau trong .env' 'Kiem tra SPRING_DATASOURCE_USERNAME/PASSWORD'
    }
    else {
        $script:MissingRequired++
        Add-Result 'Database' 'THIEU' "chua co database $DB_NAME" "CREATE DATABASE $DB_NAME CHARACTER SET utf8mb4;"
    }
}

function Check-Backend {
    $url = "http://localhost:$BE_PORT/booking-app/api/v1/ai/status"
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        if ($r.StatusCode -eq 200) {
            $enabled = 'khong ro'
            if ($r.Content -match '"enabled"\s*:\s*(true|false)') { $enabled = $Matches[1] }
            Add-Result 'Backend' 'OK' "dang chay o cong $BE_PORT, AI enabled=$enabled" ''
            return
        }
        Add-Result 'Backend' 'CANXEM' "tra ve ma $($r.StatusCode)" 'Xem log backend'
    }
    catch {
        $busy = Get-NetTCPConnection -LocalPort $BE_PORT -State Listen -ErrorAction SilentlyContinue
        if ($busy) {
            Add-Result 'Backend' 'CANXEM' "cong $BE_PORT dang bi chiem boi tien trinh khac" 'Dung tien trinh do hoac doi PORT'
        }
        else {
            Add-Result 'Backend' 'CANXEM' 'chua chay (binh thuong neu chua khoi dong)' '.\mvnw.cmd spring-boot:run'
        }
    }
}

# ---------------------------------------------------------------------------
# Chạy
# ---------------------------------------------------------------------------

Write-Host ''
Write-Host '===========================================' -ForegroundColor Cyan
Write-Host ' booking_app - kiem tra moi truong' -ForegroundColor Cyan
Write-Host '===========================================' -ForegroundColor Cyan

if ($Install) {
    if (Test-Admin) {
        Write-Host 'Che do: CAI DAT (dang chay quyen Administrator)' -ForegroundColor Yellow
    }
    else {
        Write-Host 'Che do: CAI DAT - NHUNG KHONG CO QUYEN ADMINISTRATOR.' -ForegroundColor Yellow
        Write-Host 'Mot so goi se cai that bai. Nen dong lai va mo PowerShell bang "Run as administrator".' -ForegroundColor Yellow
    }
}
else {
    Write-Host 'Che do: CHI KIEM TRA (them -Install de cai nhung thu con thieu)' -ForegroundColor Gray
}

Write-Head 'Cong cu'
Check-Winget | Out-Null
Check-Git
Check-Java
Check-MySql
Check-Flutter
Check-AndroidStudio
Check-Chrome

$root = Resolve-ProjectRoot
Write-Head "Du an (goc: $root)"
$backendDir = Check-Repos $root
if ($backendDir -and (Test-Path $backendDir)) {
    Check-EnvFile $backendDir
    Check-Database $backendDir
}
Check-Backend

# ---------------------------------------------------------------------------
# Tổng kết
# ---------------------------------------------------------------------------

Write-Head 'Tong ket'
$todo = $script:Results | Where-Object { $_.TinhTrang -ne 'OK' }
if ($todo.Count -eq 0) {
    Write-Host '  Moi thu da san sang.' -ForegroundColor Green
}
else {
    Write-Host '  Con lai phai lam:' -ForegroundColor Yellow
    foreach ($item in $todo) {
        Write-Host ("   - {0,-18} {1}" -f $item.Muc, $item.Sua) -ForegroundColor Yellow
    }
}

if ($script:MissingRequired -gt 0) {
    Write-Host ''
    Write-Host "  Con $($script:MissingRequired) muc BAT BUOC chua xong. Xem SETUP.md." -ForegroundColor Red
    if (-not $Install) {
        Write-Host '  Chay lai voi -Install de script cai giup.' -ForegroundColor Red
    }
    exit 1
}

Write-Host ''
Write-Host '  Khong thieu muc bat buoc nao.' -ForegroundColor Green
exit 0
