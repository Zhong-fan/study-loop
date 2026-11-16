-- 资料页文本：PDF 解析后的产物
-- 用来支持"搜索资料内容"和"从某段出题"

USE study_course;

CREATE TABLE IF NOT EXISTS material_page (
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    material_id BIGINT   NOT NULL COMMENT '所属资料',
    user_id     BIGINT   NOT NULL COMMENT '归属用户',
    page_no     INT      NOT NULL COMMENT '页码，从 1 开始',
    content     MEDIUMTEXT COMMENT '该页文字',
    created_at  DATETIME NOT NULL COMMENT '解析时间',
    PRIMARY KEY (id),
    KEY idx_material_page (material_id, page_no),
    KEY idx_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='资料页文本';

-- 说明：当前用 LIKE '%关键词%' 搜索，数据量在几万行以内够用。
-- 如果以后要按相关度排序，再把 content 换成 FULLTEXT + ngram 索引。
