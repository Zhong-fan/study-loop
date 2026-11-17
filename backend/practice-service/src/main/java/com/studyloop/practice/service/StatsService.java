package com.studyloop.practice.service;

import com.studyloop.practice.vo.StatsOverviewVO;
import com.studyloop.practice.vo.TrendPointVO;
import com.studyloop.practice.vo.WeakChapterVO;

import java.util.List;

/**
 * 统计业务接口。
 */
public interface StatsService {

    StatsOverviewVO overview();

    List<WeakChapterVO> weakChapters();

    List<TrendPointVO> trend(int days);
}
