# 变更日志

本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)，格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)。

分类：`Added` 新增 ｜ `Changed` 变更 ｜ `Deprecated` 弃用 ｜ `Removed` 移除 ｜ `Fixed` 修复 ｜ `Security` 安全。

---

## [Unreleased]

### Changed（v0.4）
- 需求规格升级至 v0.4：**扫描版 PDF 与图片型 PPT 纳入范围**，新增逐页 OCR 兜底
- 解析统一为一条管线：统一转 PDF → pdf.js 渲染 → 逐页取文字（原生 or OCR）
- Office 在线预览由"待定"改为必做（LibreOffice 转 PDF）
- 扫描页不做透明文字层，改为右侧"本页文字"面板

### Added（v0.4）
- ADR-0009：统一转 PDF，逐页取文字，OCR 兜底
- OCR 相关业务规则：逐页阈值、并发 2、单页超时、失败降级、页数上限 300

### Changed（v0.3）
- 需求规格升级至 v0.3：**视频解析整体延后**，解析能力转向 PPT / PDF / DOCX
- 服务拆分回退为 **3 个业务服务**（网关 + auth / course / review），`media` 不再本期拆分
- 文档解析并入 `course`，走独立线程池（核心 2 / 队列 20），超时 60 秒
- Office 在线预览改为 V2 待定，V1 只保证 PDF 与图片

### Added（v0.3）
- ADR-0008：先做文档解析，视频延后（含"ASR 走云 API"的遗留结论）
- 文档解析需求：文本抽取、页码定位、无文本层降级、全文检索、选中出题

### Deprecated（v0.3）
- ADR-0006 状态改为 `Deferred`

### Changed（v0.2）
- 题目由单一错题改为**分题型题库**，客观题由服务端判分，主观题自评
- 组卷自测从"自评练习"改为"作答 → 判分 → 回流复习卡"

### Added（v0.2）
- ADR-0006：视频解析用 yt-dlp + 本地 Whisper（现已 Deferred）
- ADR-0007：题目分型与客观题服务端判分
- 待补：M0 工程骨架、数据库初始化脚本

### Deprecated（v0.2）
- ADR-0004（视频只做 B 站外链）被 ADR-0006 取代

---

## [0.1.0] - 2026-11-07

### Added
- `README.md`：项目定位、功能分期、技术栈、里程碑
- `CONTRIBUTING.md`：分支模型、提交规范、PR 检查清单、代码规范
- `docs/01-需求规格.md`：角色与数据边界、33 条功能需求、14 条业务规则、6 个关键用例、验收演示脚本
- `docs/adr/`：ADR 机制与 0001~0005 五条初始决策
- `.gitignore` / `.gitattributes` / `.editorconfig` / `LICENSE`

### Changed
- 项目定位由"教师 + 学生双角色平台"收缩为**单角色个人学习工具**（见 ADR-0002）
- 原《架构设计 v0》归档至 `docs/archive/`，标注为已废弃

### Removed
- 教师端、选课、班级统计等需求（不在本期范围）
