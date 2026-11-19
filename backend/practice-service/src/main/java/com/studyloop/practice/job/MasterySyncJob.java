package com.studyloop.practice.job;

import com.studyloop.practice.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 掌握度投影补偿任务。
 * <p>
 * 作答成功但 insight 暂时不可用时，attempt 会带着 synced=0 落库，
 * 这里定时把它们补投过去；insight 侧用 attemptId 做幂等，重复投递不会多算。
 */
@Component
public class MasterySyncJob {

    private static final Logger LOG = LoggerFactory.getLogger(MasterySyncJob.class);

    private final ReviewService reviewService;

    public MasterySyncJob(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Scheduled(initialDelayString = "${app.mastery-sync.initial-delay-ms:30000}",
            fixedDelayString = "${app.mastery-sync.interval-ms:60000}")
    public void syncPending() {
        try {
            int count = reviewService.retryUnsynced(50);
            if (count > 0) {
                LOG.info("补投掌握度成功 {} 条", count);
            }
        } catch (Exception e) {
            LOG.warn("补投掌握度任务异常: {}", e.getMessage());
        }
    }
}
