package com.studyloop.content.client;

import com.studyloop.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 通知覆盖服务清理数据。
 * <p>
 * 删除知识点后，它的 mastery 记录就没有意义了；不清理的话覆盖率里的
 * "练过 / 已掌握" 会把已经不存在的知识点算进去。
 * 调用失败只记日志：知识点已经删了，不该因为清理失败而回滚。
 */
@FeignClient(name = "insight-service")
public interface InsightCleanupClient {

    @DeleteMapping("/api/insight/mastery/knowledge/{knowledgeId}")
    Result<Void> deleteMastery(@PathVariable("knowledgeId") Long knowledgeId,
                               @RequestHeader("X-User-Id") Long userId);
}
