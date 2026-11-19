package com.studyloop.practice.service;

import com.studyloop.practice.vo.AnswerResultVO;
import com.studyloop.practice.vo.DueQueueVO;

import java.util.List;

/**
 * 练习与判分。
 * <p>
 * 排期不在这个服务里：作答完只负责写流水并回调 insight，
 * 下次复习时间由 insight 根据知识点的掌握度算出来。
 */
public interface ReviewService {

    /** 今日待练：到期的知识点里有题的返回题目，没题的返回知识点 id */
    DueQueueVO dueQuestions(int limit);

    /** 客观题：提交作答，服务端判分 */
    AnswerResultVO answerQuestion(Long questionId, List<String> answer, Long costMs);

    /** 主观题：自评 0/1/2 分 */
    int retryUnsynced(int limit);

    AnswerResultVO rateQuestion(Long questionId, int rating, Long costMs);
}
