# CMMR 前端

课程资料管理与错题复习系统的前端，Vue 3 + Element Plus + Vite + TypeScript。

## 跑起来

```bash
npm install
npm run dev      # http://127.0.0.1:5173
```

开发服务器会把 `/api` 代理到 `http://localhost:8080`（网关），所以不需要处理跨域。
先把后端起起来：MySQL / Nacos / MinIO 用 `docker compose -f ../backend/deploy/docker-compose.yml up -d`。

## 页面

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | 登录 | 登录 / 注册 |
| `/dashboard` | 今日复习 | 逐题作答，客观题自动判分，主观题自评 |
| `/courses` | 课程与资料 | 课程、章节、上传下载、PDF 逐页文字与文档内搜索、划词出题 |
| `/questions` | 题库 | 筛选、新建、删除 |
| `/quiz` | 自测 | 随机组卷、交卷判分 |
| `/stats` | 统计 | 总览、薄弱章节、趋势 |

## 目录

```
src/
├─ api/index.ts        所有后端接口，一个文件看完
├─ utils/request.ts    axios 封装：带 token、拆 Result、统一报错
├─ router/index.ts     静态路由 + 登录守卫
├─ components/         布局：header / sidebar / tabs / menu
├─ store/              sidebar / tabs / theme
└─ views/              6 个页面
```

## 约定

- 接口统一返回 `{ code, message, data }`，`code = 0` 为成功；`request.ts` 已经把 `data` 拆出来，页面里 `await getCourses()` 拿到的就是数据。
- 新增页面：在 `views/` 建 `.vue` → 在 `router/index.ts` 加路由 → 在 `components/menu.ts` 加菜单项。

## 来源

界面骨架来自 [vue-manage-system](https://github.com/lin-xin/vue-manage-system)（MIT License，Copyright © 2019-present lin-xin），保留了它的布局、菜单、标签页与请求封装；路由、接口层和全部业务页面为本项目改写。
