<div align="center">

# CMMR

**课程资料管理与错题复习系统**

把课件（PPT / PDF / 扫描件）变成可检索的题库，按遗忘曲线推送"今日待复习"，选择题自动判分并给出薄弱章节。

[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.12-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2023.0.3-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)

</div>

> 软件工程课程设计 · 项目代号 `CMMR`（Course Material & Mistake Review），文档编号形如 `CMMR1.00概要设计说明书`。
> 当前阶段：**配置库已初始化，需求与设计文档编写中；后端骨架已建立（账号 / 网关 / 课程），资料与错题待实现。**

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [仓库结构](#仓库结构)
- [快速开始](#快速开始)
- [接口一览](#接口一览)
- [持续集成](#持续集成)
- [分支与基线](#分支与基线)
- [命名规范](#命名规范)
- [开发约定](#开发约定)

## 功能特性

| 模块 | 状态 | 说明 |
| --- | :---: | --- |
| 账号 | ✅ | 注册、登录，密码 BCrypt 加密，签发 JWT |
| 网关 | ✅ | 统一入口，校验 token 后把 `X-User-Id` 传给下游 |
| 课程 | ✅ | 增删改查、分页、按名称搜索、软删除，数据按用户隔离 |
| 资料 | ✅ | 上传、下载、归类；**PDF 上传后自动逐页抽文字，可按页查看与关键词搜索** |
| 错题 | ✅ | 六种题型、按题型校验、客观题服务端判分；**支持从资料页划词出题，记录来源页码与原文** |
| 复习 | ✅ | 今日队列、SM-2 排期、复习流水、手动标掌握 |
| 自测 | ✅ | 按课程随机抽客观题组卷，交卷自动判分，结果回流复习卡 |
| 统计 | ✅ | 总览数字、连续复习天数、薄弱章节 Top5、复习趋势 |
| 前端 | ✅ | 基于 vue-manage-system 改造：登录、今日复习、课程与资料、题库、统计 |

## 技术栈

Spring Boot · Spring Cloud Alibaba（Nacos / Gateway / OpenFeign）· MyBatis-Plus · MySQL 8 · Redis · MinIO · Vue 3

| 层 | 选型 | 版本 |
| --- | --- | --- |
| 语言 / 运行时 | Java | 17 |
| 应用框架 | Spring Boot | 3.2.12 |
| 微服务 | Spring Cloud | 2023.0.3 |
| 注册中心 | Spring Cloud Alibaba + Nacos | 2023.0.3.3 |
| 网关 | Spring Cloud Gateway | 随 Cloud |
| 数据访问 | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 文件存储 | 本地磁盘（默认）/ MinIO（`app.storage=minio`） | 8.5.12 |
| 工具 | Hutool（JWT / BCrypt）、Lombok | 5.8.32 |
| 接口文档 | Knife4j（OpenAPI 3） | 4.5.0 |

## 仓库结构

本仓库同时承担**配置库**与**代码库**两个角色：

```
.
├─ 1-过程类/                 管理库：过程与管理工作产品
│  ├─ 3配置管理/             01配置管理计划表、02配置相关表格（出库登记、版本控制、基线、基线变更、发布清单）
│  ├─ 4变更及跟踪/           06变更控制表
│  ├─ 6评审/                 1需求评审、2计划评审、3概要评审
│  ├─ 7会议纪要/
│  ├─ 8数据收集/             度量汇总表
│  └─ 9其他/                 风险管理、产品审计检查表、软件过程审计报告
├─ 2-文档类/                 开发库：阶段成果
│  ├─ 1立项/                 规划立项通知书
│  ├─ 2计划/                 软件质量保证计划
│  ├─ 3需求/                 需求规格说明书
│  ├─ 4设计/                 概要设计说明书、详细设计说明书
│  └─ 12发布/                产品发布申请表、发布通知单
├─ backend/                  微服务源码
│  ├─ pom.xml                父 POM，所有版本号只在父 POM 出现
│  ├─ study-loop-common/     返回体、异常、当前登录用户
│  ├─ study-loop-gateway/    8080 路由 + 登录校验
│  ├─ study-loop-auth/       8081 注册登录、签发 token
│  ├─ study-loop-course/     8082 课程（完整样板）
│  ├─ study-loop-review/     8083 空壳，等待填业务
│  └─ deploy/                docker-compose、建库脚本
└─ frontend/                 前端（Vue3 + Element Plus，基于 vue-manage-system 改造）
```

<details>
<summary><strong>单个服务的内部分层</strong></summary>

```
controller  → 只接参数、调 service、包 Result
service     → 业务规则、事务边界
mapper      → 只做数据访问
entity      → 和表一一对应
dto / vo    → 入参 / 出参，不直接暴露 entity
```

</details>

## 快速开始

### 1. 启动中间件

```bash
docker compose -f backend/deploy/docker-compose.yml up -d
```

MySQL 首次启动会自动执行 `backend/deploy/mysql/init/` 下的脚本，建好 `study_auth` 和 `study_course` 两个库。
Nacos 控制台：<http://localhost:8848/nacos>（默认 nacos / nacos）。

### 2. 构建

```bash
mvn -f backend/pom.xml clean package -DskipTests
```

### 3. 启动四个服务

```bash
java -jar backend/study-loop-auth/target/study-loop-auth-1.0.0.jar
java -jar backend/study-loop-course/target/study-loop-course-1.0.0.jar
java -jar backend/study-loop-review/target/study-loop-review-1.0.0.jar
java -jar backend/study-loop-gateway/target/study-loop-gateway-1.0.0.jar
```

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| gateway | 8080 | 前端只访问这个 |
| auth | 8081 | 注册登录 |
| course | 8082 | 课程 |
| review | 8083 | 错题（空壳） |

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 <http://127.0.0.1:5173>，注册一个账号就能用。
开发服务器已配置代理，`/api` 会被转发到 `http://localhost:8080`（网关），不需要处理跨域。

> 前端模板来自 [vue-manage-system](https://github.com/lin-xin/vue-manage-system)：保留了它的布局、菜单、标签页和请求封装，路由与业务页面换成了本项目的。

### 5. 验证

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test01\",\"password\":\"123456\",\"nickname\":\"小明\"}"

# 登录，拿到 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test01\",\"password\":\"123456\"}"

# 建一门课（把 <token> 换成上一步的返回值）
curl -X POST http://localhost:8080/api/courses \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d "{\"name\":\"数据结构\",\"description\":\"第三章 树\"}"
```

> Windows PowerShell 里 `curl` 是 `Invoke-WebRequest` 的别名，请改用 `curl.exe`，或直接用 Knife4j 页面调试。

接口文档：<http://localhost:8080/doc.html>（网关转发），或各服务端口下的 `/doc.html`。

## 接口一览

| 方法 | 路径 | 说明 | 需要 token |
| --- | --- | :---: | :---: |
| POST | `/api/auth/register` | 注册 | 否 |
| POST | `/api/auth/login` | 登录 | 否 |
| GET | `/api/courses` | 分页查询我的课程 | 是 |
| GET | `/api/courses/{id}` | 课程详情 | 是 |
| POST | `/api/courses` | 新建课程 | 是 |
| PUT | `/api/courses/{id}` | 修改课程 | 是 |
| DELETE | `/api/courses/{id}` | 删除课程（软删） | 是 |
| GET | `/api/courses/{courseId}/chapters` | 章节列表 | 是 |
| POST | `/api/courses/{courseId}/chapters` | 新增章节 | 是 |
| PUT | `/api/chapters/{id}` | 修改章节 | 是 |
| DELETE | `/api/chapters/{id}` | 删除章节 | 是 |
| POST | `/api/materials` | 上传资料（multipart） | 是 |
| GET | `/api/materials?courseId=` | 资料列表 | 是 |
| GET | `/api/materials/{id}/download` | 下载资料 | 是 |
| GET | `/api/materials/{id}/pages` | 查看逐页文字 | 是 |
| GET | `/api/materials/{id}/search?keyword=` | 在文档内搜关键词 | 是 |
| DELETE | `/api/materials/{id}` | 删除资料（软删） | 是 |
| POST | `/api/questions` | 新建题目（自动生成复习卡） | 是 |
| GET | `/api/questions` | 分页查询题目 | 是 |
| GET | `/api/questions/{id}` | 题目详情（含答案） | 是 |
| PUT | `/api/questions/{id}` | 修改题目 | 是 |
| DELETE | `/api/questions/{id}` | 删除题目 | 是 |
| GET | `/api/review/today` | 今日待复习 | 是 |
| POST | `/api/review/cards/{id}/answer` | 提交作答（自动判分） | 是 |
| POST | `/api/review/cards/{id}/rate` | 自评（主观题） | 是 |
| POST | `/api/quizzes` | 按课程随机组卷 | 是 |
| GET | `/api/quizzes/{id}` | 查看试卷（不含答案） | 是 |
| POST | `/api/quizzes/{id}/submit` | 交卷判分 | 是 |
| GET | `/api/review/stats/overview` | 总览：题数、待复习、连续天数 | 是 |
| GET | `/api/review/stats/weak-chapters` | 薄弱章节 Top5 | 是 |
| GET | `/api/review/stats/trend?days=30` | 复习趋势 | 是 |
| GET | `/api/review/ping` | 连通性检查 | 是 |

## 持续集成

CI 配置在 `.github/workflows/ci.yml`，PR 以及推送到 `main` / `dev` 时自动运行：

| Job | 检查内容 |
| --- | --- |
| `build · backend` | JDK 17 编译五个模块并**运行单元测试**（判分规则、SM-2 排期），成功后把 jar 作为 artifact 保留 7 天 |
| `guard · 约定检查` | 模块清单完整、禁止 `System.out`、禁止字段注入、禁止跨服务 import、每个服务必须有启动类 |
| `check · docker-compose` | `docker compose config` 校验编排文件 |

同一分支连续提交时，旧任务会自动取消（`concurrency` + `cancel-in-progress`），不会白跑。

> **注意**：GitHub Actions 只在 GitHub 上执行。当前主仓库在 Gitee，需要在 GitHub 上保留镜像仓库，把分支同时推到两边：
>
> ```bash
> git push origin <branch>    # Gitee
> git push github <branch>    # GitHub，触发 CI
> ```

## 分支与基线

| 类型 | 约定 |
|---|---|
| 分支 | `main`（受控，仅配置管理员合并）、`dev`（集成）、`feature/*`、`hotfix/*` |
| 基线标签 | `BL-01-计划-v1.0`、`BL-02-需求-v1.0`、`BL-03-设计-v1.0`、`BL-04-编码-v1.0`、`BL-05-测试-v1.0`、`REL-v1.0` |
| 提交信息 | `[类型] 描述 (#变更单)`，类型：feat / fix / docs / chore |

## 命名规范

| 对象 | 规则 | 示例 |
|---|---|---|
| 文档编号 | 代号 + 版本 + 文档名 | `CMMR1.00概要设计说明书` |
| 配置项 | `CMMR-<类型>-<序号>` | `CMMR-REQ-001` |
| 测试用例 | `TC-<功能>-<方法>-<序号>` | `TC-UP-EQ-003` |
| 缺陷 | `BUG-<序号>` | `BUG-007` |

## 开发约定

**新增一个业务（以「资料」为例）**，照着 `course` 复制这几个文件，改名字即可：

```
material/entity/Material.java
material/mapper/MaterialMapper.java
material/dto/MaterialCreateRequest.java
material/vo/MaterialVO.java
material/service/MaterialService.java
material/service/impl/MaterialServiceImpl.java
material/controller/MaterialController.java
```

几条硬性约定：

1. 所有接口返回 `Result<T>`，列表用 `PageResult<T>`，不要各写各的。
2. 业务错误抛 `BizException`，由 `GlobalExceptionHandler` 统一转成返回体。
3. 任何查询都要带 `user_id`，取当前用户用 `UserContext.requireUserId()`。
4. 服务之间不 `import` 对方的类，不跨库 JOIN。
5. `common` 只放技术性的东西，不放 `CourseDTO` 这类业务对象。
6. 删除一律软删（`deleted = 1`）。

## 说明

- 开发阶段的数据库账号密码写在 `application.yml` 里（root / root123456），上线要改成环境变量。
- JWT 的 `SECRET` 在 `auth` 和 `gateway` 各有一份，两处必须一致。
- 需求确认清单等个人工作笔记保留了本地，不纳入本仓库版本控制。
