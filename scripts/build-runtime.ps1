# 构建并把 6 个服务的 jar 收集到 runtime/
#
# 用法：
#   powershell -ExecutionPolicy Bypass -File scripts\build-runtime.ps1
#   powershell ... -WithTests     # 连测试一起跑
param(
    [switch]$WithTests
)

$ErrorActionPreference = "Stop"

$scripts = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Split-Path -Parent $scripts
$runtime = Join-Path $repo "runtime"
$pom = Join-Path $repo "backend\pom.xml"

if (-not $env:JAVA_HOME) {
    $candidate = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
    if (Test-Path $candidate) {
        $env:JAVA_HOME = $candidate
        $env:Path = "$candidate\bin;$env:Path"
    } else {
        throw "未找到 JAVA_HOME，请先安装 JDK 17 或设置 JAVA_HOME"
    }
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "未找到 mvn，请把 Maven 加入 PATH"
}

New-Item -ItemType Directory -Force -Path $runtime | Out-Null

Write-Host "== 构建 =="
$mavenArgs = @("-B", "-f", $pom, "package")
if (-not $WithTests) {
    $mavenArgs += "-DskipTests"
}
& mvn @mavenArgs
if ($LASTEXITCODE -ne 0) {
    throw "Maven 构建失败，退出码 $LASTEXITCODE"
}

$jars = @(
    @{ name = "user-service";     path = "backend\user-service\target\user-service-1.0.0.jar" },
    @{ name = "content-service";  path = "backend\content-service\target\content-service-1.0.0.jar" },
    @{ name = "practice-service"; path = "backend\practice-service\target\practice-service-1.0.0.jar" },
    @{ name = "insight-service";  path = "backend\insight-service\target\insight-service-1.0.0.jar" },
    @{ name = "ai-service";       path = "backend\ai-service\target\ai-service-1.0.0.jar" },
    @{ name = "gateway";          path = "backend\study-loop-gateway\target\study-loop-gateway-1.0.0.jar" }
)

Write-Host "== 收集产物 =="
foreach ($jar in $jars) {
    $src = Join-Path $repo $jar.path
    if (-not (Test-Path $src)) {
        throw "缺少构建产物：$($jar.path)"
    }
    Copy-Item $src (Join-Path $runtime "$($jar.name).jar") -Force
}

Get-ChildItem (Join-Path $runtime "*.jar") |
    Sort-Object Name |
    Select-Object Name, @{ n = "MB"; e = { [math]::Round($_.Length / 1MB, 1) } }, LastWriteTime |
    Format-Table -AutoSize

Write-Host "完成：jar 已同步到 $runtime"
