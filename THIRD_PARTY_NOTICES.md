# Third-party notices

本项目为课程设计，部分模块的行为与实现参考/改编自以下 MIT 项目。代码按本项目的 Java / Vue 技术栈和分库结构重写，不是整文件覆盖。

## quizzy

- 仓库：https://github.com/hhh666hhh666/quizzy
- 许可证：MIT，见 `third_party/licenses/quizzy-MIT.txt`
- 版权：Copyright (c) 2026 Zihui Luo
- 改编位置：
  - `backend/practice-service/src/main/java/com/studyloop/practice/service/QuestionExcelService.java`
  - `backend/practice-service/src/main/java/com/studyloop/practice/vo/ImportResultVO.java`
  - `backend/practice-service/src/main/java/com/studyloop/practice/vo/ImportErrorVO.java`
  - `frontend/src/views/questions.vue` 的导入弹窗
- 改动：去掉分类/标签/试卷模型，课程与章节改为请求参数；默认整批导入失败回滚，可选跳过错误行；Excel 读写改为 Apache POI。

### quizzy-web（同一仓库的 Web 前端）

- 许可证：MIT，见 `third_party/licenses/quizzy-MIT.txt`
- 版权：Copyright (c) 2026 Zihui Luo
- 改编位置：
  - `frontend/src/assets/css/quizzy.css`、`quizzy-tokens.css`
  - `frontend/src/components/ui/*`（button / input / label / dialog 等）
  - `frontend/src/lib/utils.ts`
  - `frontend/src/views/home.vue`（侧栏结构）
  - `frontend/src/views/pages/login.vue`（双栏登录表单）
  - `frontend/src/views/dashboard.vue`、`frontend/src/views/questions.vue` 的卡片与表格样式
- 改动：品牌、导航、路由、接口全部换成本项目的；移除备案号、主题切换、收藏夹、试卷、BeianFooter；未引入 Tailwind preflight，旧页面仍用 Element Plus。
