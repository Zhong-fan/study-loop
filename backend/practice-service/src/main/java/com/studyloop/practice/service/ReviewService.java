package com.studyloop.practice.service;

import com.studyloop.practice.vo.AnswerResultVO;
import com.studyloop.practice.vo.DueQuestionVO;

import java.util.List;

/**
 * 练习与判分。
 * <p>
 * 排期不在这个服务里：作答完只负责写流水并回调 insight，
 * 下次复习时间由 insight 根据知识点的掌握度算出来。
 */
public interface ReviewService {

    /** 今日待练：从覆盖服务拿到期知识点，再取这些知识点下的题 */
    List<DueQuestionVO> dueQuestions(int limit);

    /** 客观题：提交作答，服务端判分 */
    AnswerResultVO answerQuestion(Long questionId, List<String> answer, Long costMs);

    /** 主观题：自评 0/1/2 分 */
    AnswerResultVO rateQuestion(Long questionId, int rating, Long costMs);
}
