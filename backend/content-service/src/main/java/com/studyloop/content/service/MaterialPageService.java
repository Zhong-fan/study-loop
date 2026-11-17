package com.studyloop.content.service;

import com.studyloop.content.vo.MaterialPageVO;
import com.studyloop.content.vo.PageSearchHitVO;

import java.io.InputStream;
import java.util.List;

/**
 * 文档文本解析。
 */
public interface MaterialPageService {

    /**
     * 把 PDF 逐页抽成文字。
     * 解析失败不影响上传本身，调用方只需要记录日志。
     *
     * @return 实际写入的页数
     */
    int parsePdf(Long materialId, Long userId, InputStream inputStream);

    /** 某一份资料的全部页文本 */
    List<MaterialPageVO> listByMaterial(Long materialId);

    /** 在某一份资料里搜关键词 */
    List<PageSearchHitVO> search(Long materialId, String keyword);
}
