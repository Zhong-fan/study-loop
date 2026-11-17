package com.studyloop.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.content.client.AiParseClient;
import com.studyloop.content.entity.MaterialPage;
import com.studyloop.content.entity.Task;
import com.studyloop.content.mapper.MaterialPageMapper;
import com.studyloop.content.mapper.TaskMapper;
import com.studyloop.content.service.MaterialParseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面解析流水线（第一版）：
 * 已有纯文本 → 调 ai-service → 回填 blocks/emphasis → 标记状态。
 * <p>
 * 失败不静默：写 NEEDS_REVIEW 和警告，页面上能看到哪一页没解析成功。
 * 扫描件这类没有文字的页，暂时直接标 NEEDS_REVIEW（后续接视觉模型）。
 */
@Service
public class MaterialParseServiceImpl implements MaterialParseService {

    private static final Logger LOG = LoggerFactory.getLogger(MaterialParseServiceImpl.class);

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_REVIEW = "NEEDS_REVIEW";

    private final MaterialPageMapper pageMapper;
    private final TaskMapper taskMapper;
    private final AiParseClient aiParseClient;
    private final ObjectMapper objectMapper;

    public MaterialParseServiceImpl(MaterialPageMapper pageMapper, TaskMapper taskMapper,
                                    AiParseClient aiParseClient, ObjectMapper objectMapper) {
        this.pageMapper = pageMapper;
        this.taskMapper = taskMapper;
        this.aiParseClient = aiParseClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> parseMaterial(Long userId, Long materialId) {
        List<MaterialPage> pages = pageMapper.selectList(new LambdaQueryWrapper<MaterialPage>()
                .eq(MaterialPage::getMaterialId, materialId)
                .eq(MaterialPage::getUserId, userId)
                .orderByAsc(MaterialPage::getPageNo));

        if (pages.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "这份资料没有可解析的文字页（可能是扫描件）");
        }

        Task task = startTask(userId, materialId);
        int parsed = 0;
        int review = 0;
        List<String> failures = new ArrayList<>();

        for (MaterialPage page : pages) {
            if (page.getContent() == null || page.getContent().isBlank()) {
                markReview(page, List.of("这一页没有文字层，需要视觉解析（暂未开启）"));
                review++;
                continue;
            }
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("page", page.getPageNo());
                body.put("docType", page.getDocType() == null ? "PDF" : page.getDocType());
                body.put("text", page.getContent());

                JsonNode node = aiParseClient.parsePage(userId, body).getData();
                if (node == null) {
                    throw new IllegalStateException("AI 返回为空");
                }
                applyResult(page, node);
                parsed++;
            } catch (Exception e) {
                String reason = e.getMessage() == null ? "未知错误" : e.getMessage();
                LOG.warn("第 {} 页解析失败: {}", page.getPageNo(), reason);
                markReview(page, List.of("解析失败：" + reason));
                failures.add("第 " + page.getPageNo() + " 页：" + reason);
                review++;
            }
        }

        finishTask(task, failures);
        Map<String, Object> result = new HashMap<>();
        result.put("total", pages.size());
        result.put("parsed", parsed);
        result.put("needsReview", review);
        result.put("failures", failures);
        return result;
    }

    private void applyResult(MaterialPage page, JsonNode node) {
        page.setBlocks(node.path("blocks").toString());
        page.setEmphasis(node.path("emphasis").toString());
        page.setDocType(node.path("doc_type").asText(page.getDocType() == null ? "PDF" : page.getDocType()));
        page.setParseStatus(STATUS_SUCCESS);

        JsonNode warnings = node.path("warnings");
        page.setParseWarnings(warnings.isArray() && !warnings.isEmpty() ? warnings.toString() : null);
        pageMapper.updateById(page);
    }

    private void markReview(MaterialPage page, List<String> warnings) {
        page.setParseStatus(STATUS_REVIEW);
        try {
            page.setParseWarnings(objectMapper.writeValueAsString(warnings));
        } catch (Exception e) {
            page.setParseWarnings("[]");
        }
        pageMapper.updateById(page);
    }

    private Task startTask(Long userId, Long materialId) {
        Task task = new Task();
        task.setUserId(userId);
        task.setMaterialId(materialId);
        task.setType("PARSE_AI");
        task.setStatus("RUNNING");
        task.setRetryCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    private void finishTask(Task task, List<String> failures) {
        task.setStatus(failures.isEmpty() ? STATUS_SUCCESS : "FAILED");
        task.setErrorMsg(failures.isEmpty() ? null
                : String.join("; ", failures).substring(0, Math.min(250, String.join("; ", failures).length())));
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }
}
