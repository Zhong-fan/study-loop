package com.studyloop.review.service;

import com.studyloop.review.vo.AnswerResultVO;
import com.studyloop.review.vo.ReviewCardVO;

import java.util.List;

/**
 * 复习业务接口。
 */
public interface ReviewService {

    /** 今天该复习的卡，按逾期时间排序 */
    List<ReviewCardVO> today(int limit);

    /** 客观题：提交作答，服务端判分并排期 */
    AnswerResultVO submitAnswer(Long cardId, List<String> answer, Long costMs);

    /** 主观题 / 手动：直接给 0/1/2 分并排期 */
    AnswerResultVO rate(Long cardId, int rating, Long costMs);
}
