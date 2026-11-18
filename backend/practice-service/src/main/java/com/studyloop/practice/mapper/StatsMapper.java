package com.studyloop.practice.mapper;

import com.studyloop.practice.vo.TrendPointVO;
import com.studyloop.practice.vo.WeakChapterVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计用的查询。
 * <p>
 * 数据源统一是 attempt（作答流水）和 question：
 * 复习卡和复习流水已经移除，排期相关的东西在 insight 服务里。
 */
@Mapper
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM question WHERE user_id = #{userId} AND deleted = 0")
    long countQuestions(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM attempt WHERE user_id = #{userId} AND answered_at >= CURDATE()")
    long countReviewedToday(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM attempt WHERE user_id = #{userId}")
    long countAllReviews(@Param("userId") Long userId);

    /** 最近作答过的日期，倒序，最多取 400 天够用了 */
    @Select("SELECT DISTINCT DATE(answered_at) AS d FROM attempt "
            + "WHERE user_id = #{userId} ORDER BY d DESC LIMIT 400")
    List<LocalDate> recentReviewDates(@Param("userId") Long userId);

    /**
     * 薄弱章节：得分低于 1 的次数占该章总作答次数的比例。
     * question 和 attempt 都在 study_review 里，可以放心 JOIN。
     */
    @Select("SELECT q.chapter_id AS chapterId, "
            + "       SUM(CASE WHEN a.score < 1 THEN 1 ELSE 0 END) AS wrongCount, "
            + "       COUNT(*) AS totalCount "
            + "FROM attempt a "
            + "JOIN question q ON q.id = a.question_id "
            + "WHERE a.user_id = #{userId} AND q.chapter_id IS NOT NULL AND q.deleted = 0 "
            + "GROUP BY q.chapter_id "
            + "ORDER BY wrongCount DESC "
            + "LIMIT 5")
    List<WeakChapterVO> weakChapters(@Param("userId") Long userId);

    /** 按天汇总作答量，fromDate 由 Java 算好再传进来 */
    @Select("SELECT DATE(answered_at) AS statDate, "
            + "       COUNT(*) AS reviewCount, "
            + "       SUM(CASE WHEN score >= 1 THEN 1 ELSE 0 END) AS correctCount "
            + "FROM attempt "
            + "WHERE user_id = #{userId} AND answered_at >= #{fromDate} "
            + "GROUP BY DATE(answered_at) "
            + "ORDER BY statDate")
    List<TrendPointVO> trend(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate);
}
