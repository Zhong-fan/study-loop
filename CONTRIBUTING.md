# 贡献指南

面向 3~4 人的课程设计小组。目标是**四个人同时改代码还不打架**，以及让答辩老师能看懂每个人的工作量。

---

## 0. 仓库一次性设置

建仓后立刻做完，后面不用再管：

- [ ] 仓库名 `study-loop`，Public
- [ ] Description：`课程资料管理与错题复习系统 · Spring Boot + Spring Cloud 微服务`
- [ ] Topics：`spring-boot` `spring-cloud` `microservices` `mysql` `redis` `minio` `vue3` `spaced-repetition`
- [ ] 默认分支 `main`，开启分支保护：禁止直接 push、要求 1 人 Approve
- [ ] 开启 Issues，使用 PR 模板（`.github/pull_request_template.md`）
- [ ] 添加 `.gitignore` / `.gitattributes` / `.editorconfig`（已随仓库提供）
- [ ] 每人配置 `user.name` 为真实姓名，方便统计分工

> 分支保护是给自己看的：答辩时 `main` 上每个 commit 都能追溯到一次 PR，比口头说"这是我写的"有力得多。

---

## 1. 分支模型

```
main                 ← 永远可运行，只接受 PR
 ├─ feat/course-upload
 ├─ feat/review-sm2
 ├─ fix/gateway-401
 └─ docs/adr-0003
```

| 规则 | 说明 |
|---|---|
| 不直接 push `main` | 包括组长 |
| 分支名 | `<类型>/<模块>-<简述>`，全小写连字符 |
| 生命周期 | 合并后立即删除，不留长命分支 |
| 合并方式 | Squash merge，一个 PR 对应 `main` 上一个 commit |
| 同步 | 每天开始前 `git pull --rebase origin main` |

模块名固定为：`auth` `course` `review` `gateway` `web` `common` `deploy` `docs`。

---

## 2. 提交信息

格式：

```
<type>(<scope>): <subject>
```

- **type**：`feat` 新功能 ｜ `fix` 修复 ｜ `docs` 文档 ｜ `refactor` 重构 ｜ `perf` 性能 ｜ `test` 测试 ｜ `chore` 构建/依赖 ｜ `style` 格式
- **scope**：见上面的模块名，可省略
- **subject**：中文或英文都行，**动词开头，不加句号，不超过 50 字**

示例：

```
feat(review): 实现 SM-2 排期计算
fix(gateway): 修复 token 过期时返回 500 的问题
docs(adr): 新增 0004 视频只做外链的决策
chore(deps): 升级 spring boot 至 3.2.12
```

**粒度要求**：一个 commit 只做一件事，且**能编译通过**。禁止 "update"、"修改一下"、"最终版" 这类信息。

> 反例：`git commit -m "写完了"` —— 答辩时没人知道这一天你干了什么。

---

## 3. PR 要求

PR 描述必须填模板里的四项：改了什么、怎么自测、影响范围、是否同步文档。

**合并前检查（缺一不可）：**

- [ ] 本地能启动，改动涉及的接口/页面手工点过一遍
- [ ] 新增接口已在 Knife4j 里能看到，且写了 `@Operation` 说明
- [ ] 没有提交 `target/`、`node_modules/`、`.env`、密钥、本地数据库文件
- [ ] 涉及表结构变更，同步了 `deploy/mysql/init/` 下的脚本
- [ ] 涉及决策变更，补了 ADR
- [ ] 至少 1 人 Review（作者不能自审自合）

**Review 只看三件事**：边界（越权、空值）、一致性（事务、缓存）、可读性（命名、分层）。不纠结格式，格式交给 `.editorconfig`。

---

## 4. 代码规范

### 4.1 分层

```
controller  ← 只做参数校验、调用 service、返回 Result
service     ← 业务规则、事务边界
mapper      ← 只写 SQL 映射，不写业务判断
```

- Controller 里禁止出现 `QueryWrapper`；Service 里禁止出现 `HttpServletRequest`。
- 跨服务调用统一放 `client/` 包，且必须有降级方法。

### 4.2 命名

| 对象 | 规则 | 示例 |
|---|---|---|
| 类 | 大驼峰 | `ReviewCardService` |
| 方法 | 小驼峰动词开头 | `calculateNextDueDate` |
| 常量 | 全大写下划线 | `MAX_DAILY_REVIEW` |
| 数据库表 | 单数、下划线 | `review_card` |
| 数据库字段 | 下划线 | `due_date` |
| REST 路径 | 复数、名词、无动词 | `GET /api/review/cards` |
| 布尔字段 | `is_` / `has_` 前缀，禁用 `flag` | `is_mastered` |

### 4.3 必须遵守的几条

1. **所有查询带 `user_id`**，包括按 id 查单条（先查再比，不要用 `WHERE id=? AND user_id=?` 之外的写法）。
2. **响应体统一** `Result<T>`，禁止直接返回实体；实体不外泄到 Controller。
3. **异常统一抛** `BizException(code, msg)`，由全局处理器转成响应；禁止 `e.printStackTrace()`。
4. **日志用占位符**：`log.info("review card {} rated {}", id, rating)`，禁止字符串拼接。
5. **注释解释"为什么"**，不解释"是什么"。`// 这里用乐观锁，防止用户连点两次导致间隔被算两次` 是合格注释。
6. **TODO 必须带人名**：`// TODO(张三): 视频分P切换 M4 补`。
7. **时间统一** `LocalDateTime`，时区 `Asia/Shanghai`；禁止 `new Date()` 参与业务计算。

### 4.4 配置与密钥

- 密钥、数据库密码只进环境变量或 `application-local.yml`（已在 `.gitignore`）
- 仓库里只保留 `application.yml`（公共配置）+ `.env.example`（示例值）
- **一旦误提交密钥，立即改密码并重写历史**，不要只删文件了事

---

## 5. 文档义务

| 你改了什么 | 必须同步 |
|---|---|
| 接口签名 / 字段 | `docs/` 下接口说明 + Knife4j 注解 |
| 表结构 | `deploy/mysql/init/*.sql` + ER 图 |
| 需求范围 | `docs/01-需求规格.md`，版本号 +0.1 |
| 技术方案（选型、拆分、算法） | 新增一条 ADR，**不修改旧 ADR** |
| 发布一个里程碑 | `CHANGELOG.md` |

**文档不算"额外工作"**：答辩材料 80% 直接来自这些文档，写一次用两次。

---

## 6. 提交前自检脚本（习惯后 30 秒）

```bash
# 后端
mvn -q -DskipTests package

# 前端
cd study-loop-web && npm run build

# 确认没有混入不该提交的东西
git status --short
git diff --cached --stat
```

看到 `target/`、`node_modules/`、`.env`、`*.log`、`*.sql` 备份文件，一律 `git restore --staged` 掉再提交。

---

## 7. 分工与署名

- 报告里的"任务分工"按**模块**写，不按"写代码/写文档"写：`张三：网关与鉴权（gateway + auth）`。
- 每人负责的模块，其 PR 由本人发起、他人评审；跨模块改动提前在群里说一声，避免两人同时改 `common`。
- `common` 模块是公共财产，改动需全员知情。
