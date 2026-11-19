package com.studyloop.insight.service;

import com.studyloop.insight.vo.CoverageVO;
import com.studyloop.insight.vo.MasteryVO;

import java.util.List;

public interface MasteryService {

    /** 记一次作答：更新正确率，按 1/3/7/14 重排下次复习 */
    MasteryVO record(Long userId, Long knowledgeId, Long courseId, boolean correct);

    /** 按知识点拆分的作答：分别更新表层/深层掌握度 */
    MasteryVO recordDetails(Long userId, Long courseId, Long attemptId, java.util.List<com.studyloop.insight.dto.AttemptRequest.Detail> details);

    /** 今天该复习的知识点 */
    List<MasteryVO> due(Long userId);

    /** 正确率低于 0.6 的，作为盲区清单 */
    List<MasteryVO> weak(Long userId);

    /** 会背不会用：单点正确率高、综合得分低 */
    List<MasteryVO> transfer(Long userId);

    /** 覆盖率 = 已掌握知识点 / 课程知识点总数 */
    CoverageVO coverage(Long userId, Long courseId);

    /** 给练习服务的看板用：练过多少、掌握多少、今天到期多少 */
    java.util.Map<String, Object> summary(Long userId);

    /** 知识点删除后清掉它的掌握记录，避免覆盖率里出现孤儿数据 */
    void deleteByKnowledge(Long userId, Long knowledgeId);
}
