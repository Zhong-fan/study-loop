package com.studyloop.content.service;

import com.studyloop.content.dto.KnowledgeRequest;
import com.studyloop.content.vo.KnowledgeVO;

import java.util.List;

public interface KnowledgeService {

    List<KnowledgeVO> listByChapter(Long chapterId);

    List<KnowledgeVO> listByCourse(Long courseId);

    Long create(Long courseId, KnowledgeRequest request);

    void update(Long id, KnowledgeRequest request);

    void delete(Long id);

    /** 给覆盖统计用：某门课有多少个知识点 */
    long countByCourse(Long courseId);

    /** 某门课全部知识点的证据 */
    java.util.List<com.studyloop.content.vo.KnowledgeEvidenceVO> evidenceByCourse(Long courseId);

    /** 按证据算加权的知识点列表，预测卷用 */
    java.util.List<com.studyloop.content.vo.WeightedKnowledgeVO> weightedByCourse(Long courseId);
}
