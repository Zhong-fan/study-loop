package com.studyloop.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.content.entity.Material;
import com.studyloop.content.entity.MaterialPage;
import com.studyloop.content.mapper.MaterialMapper;
import com.studyloop.content.mapper.MaterialPageMapper;
import com.studyloop.content.service.MaterialPageService;
import com.studyloop.content.vo.MaterialPageVO;
import com.studyloop.content.vo.PageSearchHitVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 文本解析。
 * <p>
 * 用 PDFBox 的 PDFTextStripper 逐页抽字：
 * 只能抽"有文字层"的 PDF，扫描件抽出来是空的，
 * 这种文件会一条记录都没有，前端提示"该文件可能是扫描件"即可。
 */
@Slf4j
@Service
public class MaterialPageServiceImpl implements MaterialPageService {

    /** 单份文件最多解析多少页，防止有人传一本几百页的书把服务拖死 */
    private static final int MAX_PAGES = 200;

    /** 关键词前后各保留多少字 */
    private static final int SNIPPET_PADDING = 30;

    private final MaterialPageMapper pageMapper;
    private final MaterialMapper materialMapper;

    public MaterialPageServiceImpl(MaterialPageMapper pageMapper, MaterialMapper materialMapper) {
        this.pageMapper = pageMapper;
        this.materialMapper = materialMapper;
    }

    @Override
    public int parsePdf(Long materialId, Long userId, InputStream inputStream) {
        int saved = 0;

        try (PDDocument document = PDDocument.load(inputStream)) {
            int total = Math.min(document.getNumberOfPages(), MAX_PAGES);
            PDFTextStripper stripper = new PDFTextStripper();

            for (int pageNo = 1; pageNo <= total; pageNo++) {
                stripper.setStartPage(pageNo);
                stripper.setEndPage(pageNo);
                String text = stripper.getText(document).trim();

                // 空白页不存，省得列表里全是空行
                if (text.isEmpty()) {
                    continue;
                }

                MaterialPage page = new MaterialPage();
                page.setMaterialId(materialId);
                page.setUserId(userId);
                page.setPageNo(pageNo);
                page.setContent(text);
                page.setCreatedAt(LocalDateTime.now());
                pageMapper.insert(page);
                saved++;
            }
        } catch (Exception e) {
            // 加密 PDF、损坏文件都会走到这里；上传已经成功了，这里只记日志
            log.warn("解析 PDF 失败，materialId={}", materialId, e);
        }
        return saved;
    }

    @Override
    public List<MaterialPageVO> listByMaterial(Long materialId) {
        checkMaterial(materialId);

        QueryWrapper<MaterialPage> wrapper = new QueryWrapper<>();
        wrapper.eq("material_id", materialId);
        wrapper.orderByAsc("page_no");

        List<MaterialPageVO> result = new ArrayList<>();
        for (MaterialPage page : pageMapper.selectList(wrapper)) {
            MaterialPageVO vo = new MaterialPageVO();
            vo.setPageNo(page.getPageNo());
            vo.setContent(page.getContent());
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<PageSearchHitVO> search(Long materialId, String keyword) {
        Material material = checkMaterial(materialId);

        if (keyword == null || keyword.trim().length() < 2) {
            throw new BizException(ErrorCode.PARAM_ERROR, "关键词至少 2 个字");
        }

        QueryWrapper<MaterialPage> wrapper = new QueryWrapper<>();
        wrapper.eq("material_id", materialId);
        wrapper.like("content", keyword.trim());
        wrapper.orderByAsc("page_no");
        // 命中太多也没意义，给前 20 条
        wrapper.last("LIMIT 20");

        List<PageSearchHitVO> result = new ArrayList<>();
        for (MaterialPage page : pageMapper.selectList(wrapper)) {
            PageSearchHitVO vo = new PageSearchHitVO();
            vo.setMaterialId(materialId);
            vo.setMaterialTitle(material.getTitle());
            vo.setPageNo(page.getPageNo());
            vo.setSnippet(snippet(page.getContent(), keyword.trim()));
            result.add(vo);
        }
        return result;
    }

    private Material checkMaterial(Long materialId) {
        Long userId = UserContext.requireUserId();

        Material material = materialMapper.selectById(materialId);
        if (material == null || material.getDeleted() == 1 || !userId.equals(material.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "资料不存在");
        }
        return material;
    }

    /** 截取关键词附近的一段文字，给前端做高亮 */
    private String snippet(String content, String keyword) {
        int index = content.indexOf(keyword);
        if (index < 0) {
            return content.length() > 80 ? content.substring(0, 80) : content;
        }
        int start = Math.max(0, index - SNIPPET_PADDING);
        int end = Math.min(content.length(), index + keyword.length() + SNIPPET_PADDING);

        String prefix = start > 0 ? "..." : "";
        String suffix = end < content.length() ? "..." : "";
        return prefix + content.substring(start, end).replace("\n", " ") + suffix;
    }
}
