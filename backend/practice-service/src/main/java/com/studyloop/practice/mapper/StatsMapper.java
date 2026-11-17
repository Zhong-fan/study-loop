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
 * 这些 SQL 是聚合查询，放不进单表 Mapper，单独开一个。
 * 全部带 user_id，不会统计到别人的数据。
 */
@Mapper
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM question WHERE user_id = #{userId} AND deleted = 0")
    long countQuestions(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM question WHERE user_id = #{userId} AND deleted = 0 AND status = 1")
    long countMastered(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM review_card WHERE user_id = #{userId} AND deleted = 0 AND due_date <= CURDATE()")
    long countDueToday(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM review_log WHERE user_id = #{userId} AND reviewed_at >= CURDATE()")
    long countReviewedToday(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM review_log WHERE user_id = #{userId}")
    long countAllReviews(@Param("userId") Long userId);

    /** 最近复习过的日期，倒序，最多取 400 天够用了 */
    @Select("SELECT DISTINCT DATE(reviewed_at) AS d FROM review_log "
            + "WHERE user_id = #{userId} ORDER BY d DESC LIMIT 400")
    List<LocalDate> recentReviewDates(@Param("userId") Long userId);

    /**
     * 薄弱章节：按"评忘记的次数"倒序。
     * 三张表都在 study_review 里，可以放心 JOIN。
     */
    @Select("SELECT q.chapter_id AS chapterId, "
            + "       SUM(CASE WHEN l.rating = 0 THEN 1 ELSE 0 END) AS wrongCount, "
            + "       COUNT(*) AS totalCount "
            + "FROM review_log l "
            + "JOIN review_card c ON c.id = l.card_id "
            + "JOIN question q ON q.id = c.question_id "
            + "WHERE l.user_id = #{userId} AND q.chapter_id IS NOT NULL AND q.deleted = 0 "
            + "GROUP BY q.chapter_id "
            + "ORDER BY wrongCount DESC "
            + "LIMIT 5")
    List<WeakChapterVO> weakChapters(@Param("userId") Long userId);

    /** 按天汇总复习量，fromDate 由 Java 算好再传进来 */
    @Select("SELECT DATE(reviewed_at) AS statDate, "
            + "       COUNT(*) AS reviewCount, "
            + "       SUM(CASE WHEN rating >= 1 THEN 1 ELSE 0 END) AS correctCount "
            + "FROM review_log "
            + "WHERE user_id = #{userId} AND reviewed_at >= #{fromDate} "
            + "GROUP BY DATE(reviewed_at) "
            + "ORDER BY statDate")
    List<TrendPointVO> trend(@Param("userId") Long userId, @Param("fromDate") LocalDate fromDate);
}
