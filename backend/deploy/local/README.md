# 本地无窗口启动

这些脚本用于在 Windows 上无窗口启动后端与前端，避免弹出命令框。

## 前置

1. 仓库根目录有 `.env`（从 `.env.example` 复制）
2. 已执行 `scripts/build-runtime.ps1`，jar 已在 `runtime/`

## 文件

| 文件 | 作用 |
|---|---|
| `start-all.vbs` / `start-all.ps1` | 无窗口启动 6 个后端服务，读取仓库根 `.env` |
| `start-web.vbs` / `start-web.ps1` | 无窗口启动 Vite |
| `stop-all.vbs` / `stop-all.ps1` | 停止服务与前端 |

## 使用

把脚本放到 `runtime/`（与 jar 同目录），双击对应 `.vbs`，或：

```powershell
wscript.exe runtime\start-all.vbs
```

启动摘要写到 `runtime/startup.log`（配置来源、读取到的变量、关键项是否设置）。

## 注意

1. **不要用 `cmd` 包装**：`cmd /c run-xxx.cmd` 会留下可见控制台窗口
2. 脚本含中文，必须以 **UTF-8 with BOM** 保存，否则 PowerShell 5.1 会按 ANSI 解析并报语法错误
3. 配置只读仓库根目录 `.env`，不读 `runtime/.env`