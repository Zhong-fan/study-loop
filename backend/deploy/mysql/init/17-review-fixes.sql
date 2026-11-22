-- review 修复：软删冲突、证据重复、真题重复导入
USE study_course;

-- 幂等辅助：列/索引不存在才执行，重复跑不会报错
DROP PROCEDURE IF EXISTS sl_add_column;
DROP PROCEDURE IF EXISTS sl_add_index;
DELIMITER $$
CREATE PROCEDURE sl_add_column(IN p_schema VARCHAR(64), IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_def TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = p_schema AND table_name = p_table AND column_name = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table, '` ADD COLUMN `', p_column, '` ', p_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
CREATE PROCEDURE sl_add_index(IN p_schema VARCHAR(64), IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_def TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = p_schema AND table_name = p_table AND index_name = p_index) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table, '` ADD ', p_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;


-- 1. 知识点软删标记改为「写入自身 id」，避免同名第二次删除撞唯一键
ALTER TABLE knowledge
    MODIFY COLUMN deleted BIGINT NOT NULL DEFAULT 0 COMMENT '0=正常；删除时写入自身 id';

UPDATE knowledge SET deleted = id WHERE deleted = 1;

-- 2. 证据去重：同一知识点、同一资料页、同一句话只留一条
DELETE e1
FROM knowledge_evidence e1
         JOIN knowledge_evidence e2
              ON e1.knowledge_id = e2.knowledge_id
                  AND e1.material_id <=> e2.material_id
                  AND e1.page_no <=> e2.page_no
                  AND e1.quote <=> e2.quote
                  AND e1.id > e2.id;

CALL sl_add_index('study_course', 'knowledge_evidence', 'uk_evidence',
    'UNIQUE KEY uk_evidence (knowledge_id, material_id, page_no, quote(191))');

-- 3. 真题题干归一化，重复导入时按归一化键跳过；题目也改软删
CALL sl_add_column('study_course', 'real_exam_item', 'stem_key',
    'VARCHAR(191) NOT NULL DEFAULT '''' COMMENT ''题干归一化键''');

UPDATE real_exam_item
SET stem_key = LOWER(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(stem,
    ' ', ''), '　', ''), '，', ''), '。', ''), '？', ''))
WHERE stem_key = '';

DELETE i1
FROM real_exam_item i1
         JOIN real_exam_item i2
              ON i1.exam_id = i2.exam_id
                  AND i1.stem_key = i2.stem_key
                  AND i1.id > i2.id;

CALL sl_add_index('study_course', 'real_exam_item', 'uk_exam_stem',
    'UNIQUE KEY uk_exam_stem (exam_id, stem_key)');

CALL sl_add_column('study_course', 'real_exam_item', 'deleted',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除''');

-- 4. 同一份资料对同一张试卷只允许导入一次（AI 改写题干时 stem_key 挡不住）
CREATE TABLE IF NOT EXISTS real_exam_import (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    exam_id     BIGINT   NOT NULL COMMENT '试卷',
    material_id BIGINT   NOT NULL COMMENT '来源资料',
    item_count  INT      NOT NULL DEFAULT 0 COMMENT '本次导入题数',
    created_at  DATETIME NOT NULL COMMENT '导入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_exam_material (exam_id, material_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='真题导入记录';
DROP PROCEDURE IF EXISTS sl_add_column;
DROP PROCEDURE IF EXISTS sl_add_index;
