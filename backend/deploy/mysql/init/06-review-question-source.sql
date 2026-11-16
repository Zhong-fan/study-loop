-- 题目来源回链：记录题目来自哪份资料的第几页
-- 只在已初始化过的库上执行一次；新建库直接用 04-review.sql 里的完整建表语句

USE study_review;

ALTER TABLE question
    ADD COLUMN material_id BIGINT DEFAULT NULL COMMENT '来源资料' AFTER chapter_id,
    ADD COLUMN page_no     INT    DEFAULT NULL COMMENT '来源页码' AFTER material_id,
    ADD COLUMN quote       TEXT   DEFAULT NULL COMMENT '出题时选中的原文' AFTER page_no;
