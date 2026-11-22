# 数据库初始化脚本

## 执行方式

- **全新库**：`docker compose up -d mysql` 时，MySQL 镜像会按文件名顺序自动执行本目录下的 `.sql`
- **已有库**：按需手工执行，例如

```bash
docker cp 17-review-fixes.sql cmmr-mysql:/tmp/
docker exec cmmr-mysql bash -c "mysql -uroot -p*** --default-character-set=utf8mb4 < /tmp/17-review-fixes.sql"
```

## 幂等性

`14` ~ `18` 已改为幂等脚本：

- 建表用 `CREATE TABLE IF NOT EXISTS`
- 加列/加索引用临时存储过程 `sl_add_column` / `sl_add_index` 判断 `information_schema` 后再执行
- 数据清理（重复证据、重复真题）可重复执行
- 脚本结束会删除临时存储过程

因此这五个脚本重复执行不会报错，也不会重置已有状态。

`01` ~ `13` 是建库建表的初始脚本，按全新库设计；对已有库手工执行前请先确认。

## 权限

`14` ~ `18` 会创建临时存储过程，执行账号需要 `CREATE ROUTINE` 权限（`root` 默认具备）。

## 注意

- 脚本里的中文依赖 `utf8mb4` 连接字符集，手工执行务必加 `--default-character-set=utf8mb4`
- 不要用 `docker exec mysql -e "含中文的 SQL"`，会因控制台编码损坏内容；用文件 + 重定向
