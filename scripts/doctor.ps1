# 本地启动自检：Java、端口、容器、AI 可用性
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\doctor.ps1
#
# 退出码：0 全部通过；1 有失败项

$ErrorActionPreference = "SilentlyContinue"
$repo = Split-Path -Parent $PSScriptRoot
$results = New-Object System.Collections.ArrayList

function Add-Result {
    param([string]$Name, [string]$Status, [string]$Hint = "")
    [void]$results.Add([pscustomobject]@{ Name = $Name; Status = $Status; Hint = $Hint })
}

# 1. Java
$jdk17 = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe"
$javaExe = if (Test-Path $jdk17) { $jdk17 } else { $null }
if (-not $javaExe -and $env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $candidate) { $javaExe = $candidate }
}
if (-not $javaExe -and (Get-Command java -ErrorAction SilentlyContinue)) {
    $javaExe = (Get-Command java).Source
}
$javaVer = if ($javaExe) { (Get-Item $javaExe).VersionInfo.ProductVersion } else { "" }
if ($javaVer -match "^17\.") {
    Add-Result "Java 17" "PASS" "$javaExe ($javaVer)"
} else {
    Add-Result "Java 17" "FAIL" "需要 JDK 17（当前: $javaExe, 版本: $javaVer）"
}
if ($env:JAVA_HOME -and $env:JAVA_HOME -notmatch "17\.") {
    Add-Result "JAVA_HOME 版本" "WARN" "当前指向 $env:JAVA_HOME；构建脚本会优先使用 17，但 mvn 可能用错 JDK"
}

# 2. 端口
$ports = @(
    @{ p = 10080; name = "gateway" },
    @{ p = 10081; name = "user-service" },
    @{ p = 10082; name = "content-service" },
    @{ p = 10083; name = "practice-service" },
    @{ p = 10085; name = "insight-service" },
    @{ p = 10086; name = "ai-service" },
    @{ p = 5173;  name = "前端 Vite" }
)
foreach ($item in $ports) {
    $ok = [bool](Test-NetConnection 127.0.0.1 -Port $item.p -InformationLevel Quiet -WarningAction SilentlyContinue)
    Add-Result "端口 $($item.p) / $($item.name)" $(if ($ok) { "PASS" } else { "FAIL" }) "双击 runtime\start-all.vbs（前端用 start-web.vbs）"
}

# 3. 容器
foreach ($c in @("cmmr-mysql", "cmmr-nacos")) {
    $state = (docker inspect -f "{{.State.Status}}" $c 2>$null)
    Add-Result "容器 $c" $(if ($state -eq "running") { "PASS" } else { "FAIL" }) "docker start $c"
}

# 4. AI
$envFile = Join-Path $repo ".env"
$token = "dev-internal-token"
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | Where-Object { $_ -match "^\s*INTERNAL_TOKEN\s*=" } | ForEach-Object {
        $token = ($_ -split "=", 2)[1].Trim()
    }
}
try {
    $ai = Invoke-RestMethod -Uri "http://127.0.0.1:10086/api/ai/status" -Headers @{ "X-Internal-Token" = $token } -TimeoutSec 5
    Add-Result "AI 可用" $(if ($ai.data.usable) { "PASS" } else { "FAIL" }) "检查仓库根目录 .env 的 AI_API_KEY，然后重启 ai-service"
} catch {
    Add-Result "AI 可用" "FAIL" "ai-service 未响应，检查 runtime\ai-service.log"
}

$results | Format-Table Name, Status, Hint -AutoSize

$fail = @($results | Where-Object { $_.Status -eq "FAIL" }).Count
if ($fail -gt 0) {
    Write-Host "有 $fail 项未通过，按「Hint」列处理。" -ForegroundColor Red
    exit 1
}
Write-Host "全部通过。" -ForegroundColor Green
