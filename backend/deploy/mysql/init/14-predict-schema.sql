-- 押题方案 P2：解析结构化、知识点证据、题目多知识点、作答明细、掌握度双轨
-- 说明：init 脚本只在全新库执行一次；已有库请手动执行同文件
--
-- 归属约定：
--   study_course  —— 资料、页、知识点、证据
--   study_review  —— 题目、题目↔知识点、评分点、作答、作答明细
--   study_insight —— 掌握度
-- 跨库不写外键，靠服务层校验。

USE study_course;

-- 页面解析结果：保留结构化块、强调信号、解析状态
ALTER TABLE material_page
    ADD COLUMN blocks JSON NULL COMMENT '结构化块 JSON',
    ADD COLUMN emphasis JSON NULL COMMENT '强调信号 JSON',
    ADD COLUMN doc_type VARCHAR(20) NOT NULL DEFAULT 'PDF' COMMENT 'SLIDE/PDF/IMAGE/DOC',
    ADD COLUMN parse_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/NEEDS_REVIEW',
    ADD COLUMN parse_warnings JSON NULL COMMENT '解析警告';

-- 知识点证据：每条知识点至少一条，能指回原文
CREATE TABLE IF NOT EXISTS knowledge_evidence (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    knowledge_id BIGINT       NOT NULL COMMENT '知识点',
    material_id  BIGINT       NOT NULL COMMENT '资料',
    page_no      INT          NOT NULL COMMENT '页码',
    quote        VARCHAR(500) NOT NULL COMMENT '原文片段',
    signal_type  VARCHAR(20)  NOT NULL COMMENT 'KEYWORD/VISUAL/REPEAT/EXAMPLE/OUTLINE',
    weight       DOUBLE       NOT NULL DEFAULT 1 COMMENT '证据权重',
    verified     TINYINT      NOT NULL DEFAULT 0 COMMENT '0 未校验 1 已校验',
    created_at   DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_knowledge (knowledge_id),
    KEY idx_material_page (material_id, page_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='知识点证据';

USE study_review;

-- 题目 ↔ 知识点，多对多（和 question 同库）
CREATE TABLE IF NOT EXISTS question_knowledge (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    question_id  BIGINT      NOT NULL COMMENT '题目',
    knowledge_id BIGINT      NOT NULL COMMENT '知识点',
    role         VARCHAR(20) NOT NULL COMMENT 'PRIMARY/RELATED/CONFUSABLE',
    weight       DOUBLE      NOT NULL COMMENT '该知识点在本题的权重 0~1',
    PRIMARY KEY (id),
    UNIQUE KEY uk_q_k (question_id, knowledge_id),
    KEY idx_knowledge (knowledge_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='题目知识点关联';

-- 评分点：综合题按点给分，每个点绑知识点
CREATE TABLE IF NOT EXISTS question_rubric (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    question_id  BIGINT       NOT NULL COMMENT '题目',
    sub_no       INT          DEFAULT NULL COMMENT '综合题的小题号',
    point_text   VARCHAR(255) NOT NULL COMMENT '评分点',
    score        DOUBLE       NOT NULL COMMENT '分值',
    knowledge_id BIGINT       DEFAULT NULL COMMENT '对应知识点',
    PRIMARY KEY (id),
    KEY idx_question (question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='评分点';

-- 作答明细：把得分拆到知识点维度
CREATE TABLE IF NOT EXISTS attempt_detail (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    attempt_id    BIGINT      NOT NULL COMMENT '作答流水',
    knowledge_id  BIGINT      NOT NULL COMMENT '知识点',
    question_type VARCHAR(20) NOT NULL COMMENT '题型',
    context       VARCHAR(20) NOT NULL COMMENT 'SINGLE 单点 / COMPOSITE 综合',
    weight        DOUBLE      NOT NULL COMMENT '该知识点在本题权重',
    score         DOUBLE      NOT NULL COMMENT '该知识点得分率 0~1',
    PRIMARY KEY (id),
    KEY idx_attempt (attempt_id),
    KEY idx_knowledge (knowledge_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='作答明细（知识点维度）';

USE study_insight;

-- 掌握度双轨：单点情境 / 综合情境
ALTER TABLE mastery
    ADD COLUMN surface_accuracy DOUBLE NOT NULL DEFAULT 0 COMMENT '单点题正确率',
    ADD COLUMN deep_accuracy DOUBLE NOT NULL DEFAULT 0 COMMENT '综合题得分率',
    ADD COLUMN surface_count INT NOT NULL DEFAULT 0 COMMENT '单点题作答次数',
    ADD COLUMN deep_count INT NOT NULL DEFAULT 0 COMMENT '综合题作答次数',
    ADD COLUMN transfer_flag TINYINT NOT NULL DEFAULT 0 COMMENT '1 表示会背不会用';
