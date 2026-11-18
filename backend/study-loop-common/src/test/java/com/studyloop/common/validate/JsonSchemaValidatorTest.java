package com.studyloop.common.validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSchemaValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String VALID = """
            {
              "type": "SINGLE",
              "stem": "时间片轮转中，时间片过小会导致什么？",
              "options": ["响应变快", "切换开销变大", "不会抢占", "退化为FCFS"],
              "answer": ["B"],
              "total_score": 2,
              "difficulty": 3,
              "bloom": "UNDERSTAND",
              "source": "AI",
              "knowledge": [
                { "knowledge_id": 101, "weight": 1.0, "role": "PRIMARY" }
              ],
              "evidence": [
                { "material_id": 3, "page": 12, "quote": "时间片过小会频繁切换" }
              ],
              "explain": "该页标注了『必考』，且是例题"
            }
            """;

    private static ObjectNode validNode() throws Exception {
        return (ObjectNode) MAPPER.readTree(VALID);
    }

    @Test
    @DisplayName("合法草稿能通过校验")
    void validDraftPasses() throws Exception {
        List<String> errors = JsonSchemaValidator.validate("question-draft", validNode());
        assertTrue(errors.isEmpty(), "不应有错误: " + errors);
    }

    @Test
    @DisplayName("缺少证据会被拒绝")
    void missingEvidenceRejected() throws Exception {
        ObjectNode node = validNode();
        node.remove("evidence");

        List<String> errors = JsonSchemaValidator.validate("question-draft", node);
        assertFalse(errors.isEmpty(), "没有证据应该被拒绝");
    }

    @Test
    @DisplayName("选择题缺少选项会被拒绝")
    void choiceWithoutOptionsRejected() throws Exception {
        ObjectNode node = validNode();
        node.remove("options");

        List<String> errors = JsonSchemaValidator.validate("question-draft", node);
        assertFalse(errors.isEmpty(), "选择题没有选项应该被拒绝");
    }

    @Test
    @DisplayName("多塞字段会被拒绝")
    void unknownFieldRejected() throws Exception {
        ObjectNode node = validNode();
        node.put("hack", 1);

        List<String> errors = JsonSchemaValidator.validate("question-draft", node);
        assertFalse(errors.isEmpty(), "未知字段应该被拒绝");
    }

    @Test
    @DisplayName("非法 JSON 有明确提示")
    void brokenJsonRejected() {
        List<String> errors = JsonSchemaValidator.validateText("question-draft", "{not json");
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("不是合法 JSON"));
    }

    @Test
    @DisplayName("综合题必须有小问")
    void compositeNeedsSubQuestions() throws Exception {
        ObjectNode node = validNode();
        node.put("type", "COMPOSITE");
        node.remove("options");
        node.remove("answer");

        List<String> errors = JsonSchemaValidator.validate("question-draft", node);
        assertFalse(errors.isEmpty(), "综合题没有小问应该被拒绝");
    }
}
