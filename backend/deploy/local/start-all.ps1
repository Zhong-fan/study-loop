$ErrorActionPreference = "Stop"
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Split-Path -Parent $dir
$java = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe"
$log = Join-Path $dir "startup.log"

# 配置只有一个来源：仓库根目录 .env
$envFile = Join-Path $repo ".env"
$loaded = @()
$masked = @()
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
        $i = $_.IndexOf('=')
        if ($i -gt 0) {
            $k = $_.Substring(0, $i).Trim()
            $v = $_.Substring($i + 1).Trim()
            [Environment]::SetEnvironmentVariable($k, $v, "Process")
            $loaded += $k
            if ($k -match 'KEY|SECRET|TOKEN') {
                if ([string]::IsNullOrEmpty($v)) {
                    $masked += "$k=(空)"
                } else {
                    $masked += "$k=(已设置)"
                }
            } else {
                $masked += "$k=$v"
            }
        }
    }
}
$env:SEATA_ENABLED = "false"

$lines = @(
    "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] 启动",
    "配置来源: $envFile $(if (Test-Path $envFile) { '(存在)' } else { '(缺失，使用默认值)' })",
    "读取变量: $($loaded -join ', ')",
    "关键配置: $($masked -join ', ')",
    "SEATA_ENABLED=false"
)
$lines | Set-Content -Path $log -Encoding UTF8

$schemas = @{
    "user-service"     = "study_auth"
    "content-service"  = "study_course"
    "practice-service" = "study_review"
    "insight-service"  = "study_insight"
    "ai-service"       = "study_ai"
}

foreach ($name in $schemas.Keys) {
    $jar = Join-Path $dir "$name.jar"
    if (-not (Test-Path $jar)) { continue }
    $url = "jdbc:mysql://127.0.0.1:3307/$($schemas[$name])?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
    Start-Process -FilePath $java `
        -ArgumentList @("-jar", $jar, "--spring.datasource.url=$url") `
        -WorkingDirectory $dir -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $dir "$name.log") `
        -RedirectStandardError (Join-Path $dir "$name.err.log") | Out-Null
}

$gw = Join-Path $dir "gateway.jar"
if (Test-Path $gw) {
    Start-Process -FilePath $java -ArgumentList @("-jar", $gw) `
        -WorkingDirectory $dir -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $dir "gateway.log") `
        -RedirectStandardError (Join-Path $dir "gateway.err.log") | Out-Null
}
