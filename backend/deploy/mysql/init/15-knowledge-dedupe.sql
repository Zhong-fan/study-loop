-- 知识点归一化与别名
-- 目的：同名或同义的知识点只保留一条，证据累加，避免权重被稀释

USE study_course;

ALTER TABLE knowledge
    ADD COLUMN name_key VARCHAR(100) NOT NULL DEFAULT '' COMMENT '归一化名称键，课程内唯一';

-- 回填历史数据：去空白、去连接词、转小写
UPDATE knowledge
SET name_key = LOWER(
        REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(name,
            ' ', ''), '　', ''), '的', ''), '与', ''), '和', ''), '及', '')
    )
WHERE name_key = '';

ALTER TABLE knowledge
    ADD UNIQUE KEY uk_course_name_key (course_id, name_key, deleted);

CREATE TABLE IF NOT EXISTS knowledge_alias (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    knowledge_id BIGINT      NOT NULL COMMENT '知识点',
    alias        VARCHAR(80) NOT NULL COMMENT '别名原文',
    alias_key    VARCHAR(100) NOT NULL COMMENT '别名归一化键',
    created_at   DATETIME    NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_alias (knowledge_id, alias_key),
    KEY idx_alias_key (alias_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='知识点别名';
