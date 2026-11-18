package com.studyloop.common.validate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 页面解析与知识点候选的契约样例。
 * 这些样例同时也是给大模型的 few-shot 参考。
 */
class SchemaContractTest {

    private static final String PAGE = """
            {
              "page": 12,
              "doc_type": "SLIDE",
              "text": "3.2 进程调度",
              "blocks": [
                { "kind": "heading", "level": 2, "text": "3.2 进程调度" },
                { "kind": "bullet", "text": "时间片轮转：按时间片轮流执行" }
              ],
              "formulas": [{ "latex": "W=(\\\\sum W_i)/n", "context": "平均等待时间" }],
              "tables": [{ "caption": "调度算法对比", "rows": [["算法", "优点"], ["FCFS", "简单"]] }],
              "figures": [{ "caption": "图 3-5", "desc": "三级队列调度示意" }],
              "emphasis": [
                { "target": "时间片轮转", "signal": "KEYWORD", "quote": "必考" },
                { "target": "调度算法对比", "signal": "VISUAL", "quote": "红色加粗" }
              ],
              "raw_questions": [{ "text": "例题 3-1 ……", "has_answer": true }],
              "warnings": []
            }
            """;

    private static final String KNOWLEDGE = """
            {
              "name": "时间片轮转调度",
              "aliases": ["RR", "轮转法"],
              "chapter_title": "第3章 处理机调度",
              "parent_name": "调度算法",
              "type": "METHOD",
              "bloom": "APPLY",
              "evidence": [
                { "material_id": 3, "page": 12, "quote": "时间片轮转……", "signal": "EXAMPLE" },
                { "material_id": 3, "page": 15, "quote": "重点：时间片大小的选择", "signal": "KEYWORD" }
              ],
              "confidence": 0.86
            }
            """;

    @Test
    @DisplayName("页面解析样例通过")
    void pageDocPasses() {
        assertTrue(JsonSchemaValidator.validateText("page-doc", PAGE).isEmpty());
    }

    @Test
    @DisplayName("页面缺少页码被拒")
    void pageDocWithoutPageRejected() {
        List<String> errors = JsonSchemaValidator.validateText("page-doc",
                PAGE.replace("\"page\": 12,", ""));
        assertFalse(errors.isEmpty());
    }

    @Test
    @DisplayName("强调信号只允许约定的五种")
    void emphasisSignalRestricted() {
        List<String> errors = JsonSchemaValidator.validateText("page-doc",
                PAGE.replace("\"signal\": \"KEYWORD\"", "\"signal\": \"RANDOM\""));
        assertFalse(errors.isEmpty());
    }

    @Test
    @DisplayName("知识点候选通过")
    void knowledgePasses() {
        assertTrue(JsonSchemaValidator.validateText("knowledge-candidate", KNOWLEDGE).isEmpty());
    }

    @Test
    @DisplayName("知识点必须带证据")
    void knowledgeWithoutEvidenceRejected() {
        List<String> errors = JsonSchemaValidator.validateText("knowledge-candidate",
                KNOWLEDGE.replace("\"evidence\": [\n    { \"material_id\": 3, \"page\": 12, \"quote\": \"时间片轮转……\", \"signal\": \"EXAMPLE\" },\n    { \"material_id\": 3, \"page\": 15, \"quote\": \"重点：时间片大小的选择\", \"signal\": \"KEYWORD\" }\n  ],", "\"evidence\": [],"));
        assertFalse(errors.isEmpty());
    }
}
