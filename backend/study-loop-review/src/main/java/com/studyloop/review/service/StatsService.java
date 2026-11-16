package com.studyloop.review.service;

import com.studyloop.review.vo.StatsOverviewVO;
import com.studyloop.review.vo.TrendPointVO;
import com.studyloop.review.vo.WeakChapterVO;

import java.util.List;

/**
 * 统计业务接口。
 */
public interface StatsService {

    StatsOverviewVO overview();

    List<WeakChapterVO> weakChapters();

    List<TrendPointVO> trend(int days);
}
