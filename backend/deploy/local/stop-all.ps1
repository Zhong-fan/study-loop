$ErrorActionPreference = "SilentlyContinue"
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'cmmr-run' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Get-CimInstance Win32_Process -Filter "Name='cmd.exe'" | Where-Object { $_.CommandLine -match 'cmmr-run\\run-' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }