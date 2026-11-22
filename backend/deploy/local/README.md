# 本地无窗口启动

这些脚本用于在 Windows 上无窗口启动后端与前端，避免弹出命令框。

## 文件

| 文件 | 作用 |
|---|---|
| `start-all.vbs` | 无窗口启动 6 个后端服务 |
| `start-web.vbs` | 无窗口启动 Vite |
| `stop-all.vbs` | 停止服务 |
| `start-all.ps1` | 实际逻辑：`Start-Process java -WindowStyle Hidden` |
| `start-web.ps1` | 实际逻辑：隐藏启动 node |
| `stop-all.ps1` | 实际逻辑：按命令行匹配结束进程 |

## 使用

把脚本放到 `C:\cmmr-run`（与 jar 同目录），双击对应 `.vbs` 即可。
也可以直接运行：

```powershell
wscript.exe C:\cmmr-run\start-all.vbs
```

## 注意

1. **不要用 `cmd` 包装**：`cmd /c run-xxx.cmd` 会留下可见的控制台窗口
2. `start-web.ps1` 含中文路径，必须保存为 **UTF-8 with BOM**（PowerShell 5.1 默认按 ANSI 读取）
3. 路径按本机实际情况修改：`$repo`、`$java`、`$dir`
