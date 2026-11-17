package com.studyloop.insight.support;

import com.studyloop.insight.schedule.ReviewScheduler;

/**
 * 掌握度判定规则。
 * <p>
 * 把"什么时候算会背不会用""这次该不该升档"从服务里拆出来，
 * 一是能单测，二是规则集中在一处，改的时候不容易漏。
 */
public final class MasteryRule {

    /** 单点正确率与综合得分率相差多少算"会背不会用" */
    public static final double TRANSFER_GAP = 0.3;

    private MasteryRule() {
    }

    /**
     * 两个情境都要有足够样本才下结论：
     * 单点至少 2 次、综合至少 1 次，且差距达到阈值。
     */
    public static boolean isTransfer(double surfaceAccuracy, int surfaceCount,
                                     double deepAccuracy, int deepCount) {
        return surfaceCount >= 2 && deepCount >= 1
                && surfaceAccuracy - deepAccuracy >= TRANSFER_GAP;
    }

    /**
     * 得分满且不算"会背不会用"才升档；
     * 低于 0.5 直接归零；中间档保持不动。
     */
    public static int nextStage(int stage, double score, boolean transfer) {
        if (score >= 1.0 && !transfer) {
            return ReviewScheduler.nextStage(stage, true);
        }
        if (score < 0.5) {
            return 0;
        }
        return stage;
    }
}
