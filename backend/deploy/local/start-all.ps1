$ErrorActionPreference = "Stop"
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Split-Path -Parent $dir
$java = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe"

# 读取 .env（AI 等配置都在这里）
$envFile = Join-Path $dir ".env"
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
        $i = $_.IndexOf('=')
        if ($i -gt 0) {
            $k = $_.Substring(0, $i).Trim()
            $v = $_.Substring($i + 1).Trim()
            [Environment]::SetEnvironmentVariable($k, $v, "Process")
        }
    }
}
$env:SEATA_ENABLED = "false"

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