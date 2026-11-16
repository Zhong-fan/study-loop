package com.studyloop.review.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON 数组和 List&lt;String&gt; 互转。
 * <p>
 * 题目的 options / answer 在库里是 JSON 字符串，业务代码里用 List，
 * 转换只发生在这一处，别的地方不要自己 new ObjectMapper。
 */
public class JsonConverter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonConverter() {
    }

    public static String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            // 只有 List<String> 会走到这里，正常情况下不会失败
            throw new IllegalArgumentException("序列化失败", e);
        }
    }

    public static List<String> toList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            // 数据坏了也不要让整个列表 500，返回空列表继续跑
            return new ArrayList<>();
        }
    }
}
