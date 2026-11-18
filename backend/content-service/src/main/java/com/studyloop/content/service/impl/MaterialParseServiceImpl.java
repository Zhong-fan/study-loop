package com.studyloop.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.content.client.AiParseClient;
import com.studyloop.content.entity.Material;
import com.studyloop.content.entity.MaterialPage;
import com.studyloop.content.entity.Task;
import com.studyloop.content.mapper.MaterialMapper;
import com.studyloop.content.mapper.MaterialPageMapper;
import com.studyloop.content.mapper.TaskMapper;
import com.studyloop.content.service.MaterialParseService;
import com.studyloop.content.storage.FileStorage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 页面解析流水线：
 * 1. 有文字层的页 → 直接发文字；
 * 2. 整份资料没有文字层（扫描件）→ 把 PDF 每页渲染成图片，走视觉解析。
 * <p>
 * 失败不静默：写 NEEDS_REVIEW 和警告，页面上能看到哪一页没解析成功。
 */
@Service
public class MaterialParseServiceImpl implements MaterialParseService {

    private static final Logger LOG = LoggerFactory.getLogger(MaterialParseServiceImpl.class);

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_REVIEW = "NEEDS_REVIEW";

    /** 渲染 DPI：太低看不清，太高图片过大，140 够用 */
    private static final int RENDER_DPI = 140;

    private final MaterialPageMapper pageMapper;
    private final MaterialMapper materialMapper;
    private final TaskMapper taskMapper;
    private final AiParseClient aiParseClient;
    private final FileStorage fileStorage;
    private final ObjectMapper objectMapper;

    public MaterialParseServiceImpl(MaterialPageMapper pageMapper, MaterialMapper materialMapper,
                                    TaskMapper taskMapper, AiParseClient aiParseClient,
                                    FileStorage fileStorage, ObjectMapper objectMapper) {
        this.pageMapper = pageMapper;
        this.materialMapper = materialMapper;
        this.taskMapper = taskMapper;
        this.aiParseClient = aiParseClient;
        this.fileStorage = fileStorage;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> parseMaterial(Long userId, Long materialId) {
        List<MaterialPage> pages = pageMapper.selectList(new LambdaQueryWrapper<MaterialPage>()
                .eq(MaterialPage::getMaterialId, materialId)
                .eq(MaterialPage::getUserId, userId)
                .orderByAsc(MaterialPage::getPageNo));

        boolean hasText = pages.stream().anyMatch(p -> p.getContent() != null && !p.getContent().isBlank());
        if (!hasText) {
            // 没有文字层，走视觉解析
            return parseScanned(userId, materialId);
        }

        Task task = startTask(userId, materialId);
        int parsed = 0;
        int review = 0;
        List<String> failures = new ArrayList<>();

        for (MaterialPage page : pages) {
            if (page.getContent() == null || page.getContent().isBlank()) {
                // 混合 PDF：整份有文字，但个别页是扫描插图，单页走视觉
                try {
                    JsonNode node = parsePageByVision(userId, materialId, page.getPageNo());
                    applyResult(page, node);
                    parsed++;
                } catch (Exception e) {
                    markReview(page, List.of("这一页没有文字层，视觉解析也失败：" + e.getMessage()));
                    review++;
                }
                continue;
            }
            try {
                JsonNode node = aiParseClient.parsePage(userId, textBody(page)).getData();
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
        return summary(pages.size(), parsed, review, failures);
    }

    /** 只渲染 PDF 的某一页，交给视觉模型（用于混合 PDF 的空白页） */
    private JsonNode parsePageByVision(Long userId, Long materialId, int pageNo) throws Exception {
        Material material = materialMapper.selectById(materialId);
        if (material == null || !userId.equals(material.getUserId())) {
            throw new IllegalStateException("资料不存在");
        }
        try (InputStream in = fileStorage.open(material.getFileKey());
             PDDocument document = PDDocument.load(in)) {
            if (pageNo < 1 || pageNo > document.getNumberOfPages()) {
                throw new IllegalStateException("页码超出范围");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageNo - 1, RENDER_DPI);

            Map<String, Object> body = new HashMap<>();
            body.put("page", pageNo);
            body.put("docType", "PDF");
            body.put("text", "");
            body.put("image", toDataUrl(image));

            JsonNode node = aiParseClient.parsePage(userId, body).getData();
            if (node == null) {
                throw new IllegalStateException("AI 返回为空");
            }
            return node;
        }
    }

    /** 单张图片资料：直接读文件，一页解析 */
    private Map<String, Object> parseSingleImage(Long userId, Material material) {
        Task task = startTask(userId, material.getId());
        try (InputStream in = fileStorage.open(material.getFileKey())) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IllegalStateException("图片读不出来");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("page", 1);
            body.put("docType", "IMAGE");
            body.put("text", "");
            body.put("image", toDataUrl(image));

            JsonNode node = aiParseClient.parsePage(userId, body).getData();
            if (node == null) {
                throw new IllegalStateException("AI 返回为空");
            }
            MaterialPage page = findOrCreatePage(userId, material.getId(), 1);
            page.setDocType("IMAGE");
            applyResult(page, node);
            finishTask(task, List.of());
            return summary(1, 1, 0, List.of());
        } catch (Exception e) {
            finishTask(task, List.of(e.getMessage() == null ? "图片解析失败" : e.getMessage()));
            throw new BizException(ErrorCode.SYSTEM_ERROR, "图片解析失败：" + e.getMessage());
        }
    }

    /**
     * 扫描件：逐页渲染成 PNG，再交给多模态模型。
     * 每页都写 material_page，page_no 和 PDF 页码一致。
     */
    private Map<String, Object> parseScanned(Long userId, Long materialId) {
        Material material = materialMapper.selectById(materialId);
        if (material == null || !userId.equals(material.getUserId()) || Integer.valueOf(1).equals(material.getDeleted())) {
            throw new BizException(ErrorCode.NOT_FOUND, "资料不存在");
        }
        String fileType = material.getFileType() == null ? "" : material.getFileType().toLowerCase();
        boolean isPdf = "pdf".equals(fileType);
        boolean isImage = fileType.equals("png") || fileType.equals("jpg")
                || fileType.equals("jpeg") || fileType.equals("webp");
        if (!isPdf && !isImage) {
            throw new BizException(ErrorCode.PARAM_ERROR, "视觉解析支持 PDF 与图片");
        }
        if (isImage) {
            return parseSingleImage(userId, material);
        }

        Task task = startTask(userId, materialId);
        int parsed = 0;
        int review = 0;
        int total = 0;
        List<String> failures = new ArrayList<>();

        try (InputStream in = fileStorage.open(material.getFileKey());
             PDDocument document = PDDocument.load(in)) {

            PDFRenderer renderer = new PDFRenderer(document);
            total = document.getNumberOfPages();

            for (int i = 0; i < total; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, RENDER_DPI);
                String dataUrl = toDataUrl(image);

                Map<String, Object> body = new HashMap<>();
                body.put("page", i + 1);
                body.put("docType", "PDF");
                body.put("text", "");
                body.put("image", dataUrl);

                try {
                    JsonNode node = aiParseClient.parsePage(userId, body).getData();
                    if (node == null) {
                        throw new IllegalStateException("AI 返回为空");
                    }
                    MaterialPage page = findOrCreatePage(userId, materialId, i + 1);
                    applyResult(page, node);
                    parsed++;
                } catch (Exception e) {
                    String reason = e.getMessage() == null ? "未知错误" : e.getMessage();
                    LOG.warn("第 {} 页视觉解析失败: {}", i + 1, reason);
                    MaterialPage page = findOrCreatePage(userId, materialId, i + 1);
                    markReview(page, List.of("视觉解析失败：" + reason));
                    failures.add("第 " + (i + 1) + " 页：" + reason);
                    review++;
                }
            }
        } catch (Exception e) {
            finishTask(task, List.of(e.getMessage() == null ? "文件打不开" : e.getMessage()));
            throw new BizException(ErrorCode.SYSTEM_ERROR, "读取 PDF 失败：" + e.getMessage());
        }

        finishTask(task, failures);
        return summary(total, parsed, review, failures);
    }

    private Map<String, Object> textBody(MaterialPage page) {
        Map<String, Object> body = new HashMap<>();
        body.put("page", page.getPageNo());
        body.put("docType", page.getDocType() == null ? "PDF" : page.getDocType());
        body.put("text", page.getContent());
        return body;
    }

    private MaterialPage findOrCreatePage(Long userId, Long materialId, int pageNo) {
        MaterialPage existing = pageMapper.selectOne(new LambdaQueryWrapper<MaterialPage>()
                .eq(MaterialPage::getMaterialId, materialId)
                .eq(MaterialPage::getPageNo, pageNo)
                .last("limit 1"));
        if (existing != null) {
            return existing;
        }
        MaterialPage page = new MaterialPage();
        page.setMaterialId(materialId);
        page.setUserId(userId);
        page.setPageNo(pageNo);
        page.setContent("");
        page.setDocType("PDF");
        page.setParseStatus("PENDING");
        page.setCreatedAt(LocalDateTime.now());
        pageMapper.insert(page);
        return page;
    }

    private String toDataUrl(BufferedImage image) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private void applyResult(MaterialPage page, JsonNode node) {
        // 列是 JSON 类型，不能写空串；模型没给就存 []
        JsonNode blocks = node.path("blocks");
        page.setBlocks(blocks.isArray() ? blocks.toString() : "[]");
        JsonNode emphasis = node.path("emphasis");
        page.setEmphasis(emphasis.isArray() ? emphasis.toString() : "[]");
        page.setDocType(node.path("doc_type").asText(page.getDocType() == null ? "PDF" : page.getDocType()));
        page.setParseStatus(STATUS_SUCCESS);
        // 视觉解析出来的文字回填，方便后续搜索和知识点抽取
        if (node.path("text").isTextual() && !node.path("text").asText().isBlank()) {
            page.setContent(node.path("text").asText());
        }

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
        task.setErrorMsg(failures.isEmpty() ? null : truncate(String.join("; ", failures), 250));
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private Map<String, Object> summary(int total, int parsed, int review, List<String> failures) {
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("parsed", parsed);
        result.put("needsReview", review);
        result.put("failures", failures);
        return result;
    }

    private String truncate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
