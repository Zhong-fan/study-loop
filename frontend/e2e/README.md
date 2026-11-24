# E2E 冒烟测试（Playwright）

覆盖主路径：登录/注册校验 → 管理员登录 → 概览数据面板 → 做题 → 资料与工具 → 统计 → 用户管理，并断言浏览器无脚本异常。

## 前置

1. 中间件与后端已启动：`runtime\start-all.vbs`
2. 前端用生产构建 + preview（避免 Vite 依赖预构建在测试中途整页重载）：

```powershell
cd frontend
npm run build
npx vite preview --host 127.0.0.1 --port 4173
```

## 运行

```powershell
cd frontend
npm run test:e2e
```

默认访问 `http://127.0.0.1:4173`，可用环境变量覆盖：

```powershell
$env:E2E_BASE = "http://127.0.0.1:5173"
npm run test:e2e
```

测试使用系统已安装的 Microsoft Edge（`channel: 'msedge'`），不额外下载 Chromium。

## 失败现场

失败时截图写到 `runtime/_payload/e2e/`（该目录已被 gitignore）。

## 截图

`node e2e/shots.mjs` 会把主要页面截图写到 `runtime/_payload/redesign/`，用于视觉回归对照。
