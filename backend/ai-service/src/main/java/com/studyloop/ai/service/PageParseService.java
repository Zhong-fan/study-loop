package com.studyloop.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.studyloop.ai.dto.PageParseRequest;

public interface PageParseService {

    /**
     * 把一页文字解析成结构化页面（PageDoc）。
     * 输出必须通过 page-doc Schema 校验，不合格会带错误重试一次。
     */
    JsonNode parse(PageParseRequest request);
}
