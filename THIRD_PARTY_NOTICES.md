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
