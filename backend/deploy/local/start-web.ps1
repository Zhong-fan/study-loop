$ErrorActionPreference = "Stop"
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repo = Split-Path -Parent $dir
$env:TEMP = Join-Path $repo "runtime\tmp"
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force -Path $env:TEMP | Out-Null
Start-Process -FilePath "E:\Node\node.exe" `
    -ArgumentList @((Join-Path $repo "frontend\node_modules\vite\bin\vite.js"), "--host", "127.0.0.1", "--port", "5173") `
    -WorkingDirectory (Join-Path $repo "frontend") -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $dir "web.log") `
    -RedirectStandardError (Join-Path $dir "web.err.log") | Out-Null