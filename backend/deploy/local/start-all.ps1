$ErrorActionPreference = "Stop"
$dir = "C:\cmmr-run"
$java = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot\bin\java.exe"
$env:SEATA_ENABLED = "false"

$schemas = @{
    "user-service"     = "study_auth"
    "content-service"  = "study_course"
    "practice-service" = "study_review"
    "insight-service"  = "study_insight"
    "ai-service"       = "study_ai"
}

foreach ($name in @("user-service","content-service","practice-service","insight-service","ai-service")) {
    $jar = Join-Path $dir "$name.jar"
    if (-not (Test-Path $jar)) { continue }
    $schema = $schemas[$name]
    $url = "jdbc:mysql://127.0.0.1:3307/$schema`?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
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