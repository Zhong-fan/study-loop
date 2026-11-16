package com.studyloop.review.service.impl;

import com.studyloop.common.context.UserContext;
import com.studyloop.review.mapper.StatsMapper;
import com.studyloop.review.service.StatsService;
import com.studyloop.review.util.StreakCalculator;
import com.studyloop.review.vo.StatsOverviewVO;
import com.studyloop.review.vo.TrendPointVO;
import com.studyloop.review.vo.WeakChapterVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 统计业务实现。
 * <p>
 * 全部是实时查询，没有做汇总表：几千条复习记录，
 * 一条聚合 SQL 也就几毫秒，加汇总表反而要处理"什么时候更新"的一致性问题。
 */
@Service
public class StatsServiceImpl implements StatsService {

    private final StatsMapper statsMapper;

    public StatsServiceImpl(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    @Override
    public StatsOverviewVO overview() {
        Long userId = UserContext.requireUserId();

        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTotalQuestions(statsMapper.countQuestions(userId));
        vo.setMasteredQuestions(statsMapper.countMastered(userId));
        vo.setDueToday(statsMapper.countDueToday(userId));
        vo.setReviewedToday(statsMapper.countReviewedToday(userId));
        vo.setTotalReviews(statsMapper.countAllReviews(userId));
        vo.setStreakDays(StreakCalculator.calculate(
                statsMapper.recentReviewDates(userId), LocalDate.now()));
        return vo;
    }

    @Override
    public List<WeakChapterVO> weakChapters() {
        Long userId = UserContext.requireUserId();

        List<WeakChapterVO> list = statsMapper.weakChapters(userId);
        for (WeakChapterVO item : list) {
            if (item.getTotalCount() > 0) {
                item.setWrongRate((double) item.getWrongCount() / item.getTotalCount());
            }
        }
        return list;
    }

    @Override
    public List<TrendPointVO> trend(int days) {
        Long userId = UserContext.requireUserId();

        int range = days <= 0 ? 30 : Math.min(days, 180);
        LocalDate fromDate = LocalDate.now().minusDays(range);

        List<TrendPointVO> list = statsMapper.trend(userId, fromDate);
        for (TrendPointVO item : list) {
            if (item.getReviewCount() > 0) {
                item.setAccuracy((double) item.getCorrectCount() / item.getReviewCount());
            }
        }
        return list;
    }
}
