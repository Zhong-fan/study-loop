package com.studyloop.practice.service.impl;

import com.studyloop.common.context.UserContext;
import com.studyloop.common.result.Result;
import com.studyloop.practice.client.InsightClient;
import com.studyloop.practice.mapper.StatsMapper;
import com.studyloop.practice.service.StatsService;
import com.studyloop.practice.util.StreakCalculator;
import com.studyloop.practice.vo.StatsOverviewVO;
import com.studyloop.practice.vo.TrendPointVO;
import com.studyloop.practice.vo.WeakChapterVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计业务实现。
 * <p>
 * 作答数据在本库里直接聚合；"今天到期"和"已掌握"由 insight 服务提供，
 * 因为排期和掌握度都在那边。insight 挂了就退化成 0，不影响其他数字。
 */
@Service
public class StatsServiceImpl implements StatsService {

    private static final Logger LOG = LoggerFactory.getLogger(StatsServiceImpl.class);

    private final StatsMapper statsMapper;
    private final InsightClient insightClient;

    public StatsServiceImpl(StatsMapper statsMapper, InsightClient insightClient) {
        this.statsMapper = statsMapper;
        this.insightClient = insightClient;
    }

    @Override
    public StatsOverviewVO overview() {
        Long userId = UserContext.requireUserId();

        StatsOverviewVO vo = new StatsOverviewVO();
        vo.setTotalQuestions(statsMapper.countQuestions(userId));
        vo.setReviewedToday(statsMapper.countReviewedToday(userId));
        vo.setTotalReviews(statsMapper.countAllReviews(userId));
        vo.setStreakDays(StreakCalculator.calculate(
                statsMapper.recentReviewDates(userId), LocalDate.now()));

        try {
            Result<Map<String, Object>> summary = insightClient.summary(userId);
            if (summary != null && summary.getData() != null) {
                vo.setDueToday(asLong(summary.getData().get("dueToday")));
                vo.setMasteredQuestions(asLong(summary.getData().get("mastered")));
            }
        } catch (Exception e) {
            LOG.warn("取掌握度汇总失败，看板退化为 0: {}", e.getMessage());
        }
        return vo;
    }

    private long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
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
