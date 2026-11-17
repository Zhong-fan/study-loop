package com.studyloop.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyloop.ai.client.LlmClient;
import com.studyloop.ai.config.AiProperties;
import com.studyloop.ai.dto.PageParseRequest;
import com.studyloop.ai.entity.AiLog;
import com.studyloop.ai.mapper.AiLogMapper;
import com.studyloop.ai.service.PageParseService;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.validate.JsonSchemaValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 页面解析：把一页原文交给模型，产出 PageDoc。
 * <p>
 * 关键约束：
 * 1. 只允许输出 JSON，多余内容剥掉；
 * 2. 必须过 page-doc Schema，不过就带错误信息重试一次；
 * 3. 页码和原文由服务端回填，避免模型改页码。
 */
@Service
public class PageParseServiceImpl implements PageParseService {

    private static final Logger LOG = LoggerFactory.getLogger(PageParseServiceImpl.class);

    private static final String SYSTEM_PROMPT = """
            你是一位课件解析器。你只输出 JSON 对象，不要输出解释、不要用 markdown 代码块。
            字段必须严格符合给定的结构，signal 只能是 KEYWORD/VISUAL/REPEAT/EXAMPLE/OUTLINE。
            """;

    private final LlmClient llmClient;
    private final AiProperties properties;
    private final AiLogMapper aiLogMapper;
    private final ObjectMapper objectMapper;

    public PageParseServiceImpl(LlmClient llmClient, AiProperties properties,
                                AiLogMapper aiLogMapper, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.properties = properties;
        this.aiLogMapper = aiLogMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode parse(PageParseRequest request) {
        if (!properties.usable()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR,
                    "AI 未配置：设置环境变量 AI_API_KEY 并把 app.ai.enabled 打开");
        }
        boolean hasImage = request.getImage() != null && !request.getImage().isBlank();
        boolean hasText = request.getText() != null && !request.getText().isBlank();
        if (!hasImage && !hasText) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文字和图片至少要有一个");
        }

        String prompt = buildPrompt(request, hasImage);
        long start = System.currentTimeMillis();

        JsonNode parsed = null;
        List<String> errors = List.of();

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String content = hasImage
                        ? llmClient.chatWithImage(SYSTEM_PROMPT, prompt + retryHint(errors), request.getImage())
                        : llmClient.chat(SYSTEM_PROMPT, prompt + retryHint(errors));
                JsonNode node = objectMapper.readTree(stripFence(content));

                if (!node.isObject()) {
                    errors = List.of("根节点必须是对象");
                } else {
                    // 页码/类型以服务端为准，避免模型改
                    ((ObjectNode) node).put("page", request.getPage());
                    if (node.path("doc_type").isMissingNode()) {
                        ((ObjectNode) node).put("doc_type", request.getDocType());
                    }
                    errors = JsonSchemaValidator.validate("page-doc", node);
                    if (errors.isEmpty()) {
                        parsed = node;
                        writeLog(request, start, true, null);
                        return parsed;
                    }
                }
            } catch (Exception e) {
                errors = List.of(e.getMessage() == null ? "未知错误" : e.getMessage());
                LOG.warn("第 {} 次页面解析失败: {}", attempt, errors);
            }
        }

        writeLog(request, start, false, String.join("; ", errors));
        throw new BizException(ErrorCode.SYSTEM_ERROR, "页面解析失败：" + String.join("; ", errors));
    }

    private String buildPrompt(PageParseRequest request, boolean hasImage) {
        String source = hasImage
                ? "这一页是扫描件，请先识别图片里的文字，再按下面的结构解析。识别不准的地方写进 warnings。"
                : "这一页的原文如下，请按下面的结构解析。";
        String raw = hasImage ? "（无文字层，见图片）" : String.valueOf(request.getText());

        return """
                请把下面这一页内容解析成 JSON，结构如下（不要省略字段）：
                {
                  "page": %d,
                  "doc_type": "%s",
                  "text": "整页纯文本",
                  "blocks": [{"kind":"heading|paragraph|bullet|code|note","level":1,"text":"..."}],
                  "formulas": [{"latex":"...","context":"..."}],
                  "tables": [{"caption":"...","rows":[["表头","..."],["值","..."]]}],
                  "figures": [{"caption":"...","desc":"..."}],
                  "emphasis": [{"target":"被强调的内容","signal":"KEYWORD|VISUAL|REPEAT|EXAMPLE|OUTLINE","quote":"证据原文"}],
                  "raw_questions": [{"text":"例题/思考题原文","has_answer":true,"answer_hint":"..."}],
                  "warnings": ["识别不确定的地方"]
                }

                %s
                规则：
                - 只依据原文，不要添加原文没有的知识；
                - 原文里出现「重点/必考/掌握/考点」等词，记入 emphasis，signal 用 KEYWORD；
                - 出现「例题/练习/思考题」，记入 raw_questions，并在 emphasis 里加一条 EXAMPLE；
                - 公式用 LaTeX，表格保留行列结构；
                - 识别不清的地方写进 warnings，不要猜。

                %s（第 %d 页）：
                %s
                """.formatted(request.getPage(), request.getDocType(), source, hasImage ? "图片" : "原文",
                request.getPage(), raw);
    }

    private String retryHint(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            return "";
        }
        return "\n\n上一次输出不符合要求，错误如下，请修正后重新输出完整 JSON：\n- "
                + String.join("\n- ", errors);
    }

    private String stripFence(String content) {
        String text = content == null ? "" : content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private void writeLog(PageParseRequest request, long start, boolean success, String error) {
        try {
            AiLog log = new AiLog();
            log.setUserId(0L);
            log.setScene("PARSE_PAGE");
            log.setModel(properties.getModel());
            log.setCostMs(System.currentTimeMillis() - start);
            log.setSuccess(success ? 1 : 0);
            log.setErrorMsg(error == null ? null : error.substring(0, Math.min(error.length(), 250)));
            log.setCreatedAt(LocalDateTime.now());
            aiLogMapper.insert(log);
        } catch (Exception e) {
            LOG.warn("写 AI 调用记录失败: {}", e.getMessage());
        }
    }
}
