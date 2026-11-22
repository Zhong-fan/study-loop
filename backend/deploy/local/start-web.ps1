$ErrorActionPreference = "Stop"
$repo = "D:\1A浙江科技大学\A大三上复习\课设"
$env:TEMP = "C:\dsh-tmp"
$env:TMP = "C:\dsh-tmp"
Start-Process -FilePath "E:\Node\node.exe" `
    -ArgumentList @("$repo\frontend\node_modules\vite\bin\vite.js", "--host", "127.0.0.1", "--port", "5173") `
    -WorkingDirectory "$repo\frontend" -WindowStyle Hidden `
    -RedirectStandardOutput "C:\cmmr-run\web.log" `
    -RedirectStandardError "C:\cmmr-run\web.err.log" | Out-Null