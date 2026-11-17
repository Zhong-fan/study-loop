package com.studyloop.content.vo;

import lombok.Data;

/**
 * 搜索命中的一段文字。
 * <p>
 * snippet 是关键词前后各截一部分，避免把整页几万字都返回给前端。
 */
@Data
public class PageSearchHitVO {

    private Long materialId;

    private String materialTitle;

    private Integer pageNo;

    private String snippet;
}
