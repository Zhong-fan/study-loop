package com.studyloop.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.studyloop.practice.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目表的数据访问。
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 错题本：每道题只看最后一次作答，最后一次仍不是满分就算错题。
     * 用窗口函数而不是 GROUP_CONCAT，避免长历史被截断。
     */
    @Select("""
            SELECT q.* FROM question q
            JOIN (
                SELECT question_id, score,
                       ROW_NUMBER() OVER (PARTITION BY question_id ORDER BY answered_at DESC, id DESC) AS rn
                FROM attempt
                WHERE user_id = #{userId}
            ) a ON a.question_id = q.id AND a.rn = 1
            WHERE q.user_id = #{userId} AND q.deleted = 0 AND a.score < 1
            ORDER BY q.updated_at DESC
            LIMIT 200
            """)
    List<Question> selectWrongBook(@Param("userId") Long userId);
}
