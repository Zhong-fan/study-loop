package com.studyloop.content.support;

/**
 * 知识点权重计算。
 * <p>
 * 单独抽出来是为了能测：公式一旦被改动，测试会立刻发现。
 * 所有输入都来自证据，权重高就意味着"老师更可能考"。
 */
public final class KnowledgeWeightCalculator {

    private KnowledgeWeightCalculator() {
    }

    /**
     * @param importance    重要度 1~5
     * @param keywordCount  强调词命中次数
     * @param exampleCount  例题命中次数
     * @param repeatCount   重复出现次数
     * @param evidenceCount 证据总条数
     * @return 0~1，保留两位小数
     */
    public static double compute(int importance, int keywordCount, int exampleCount,
                                 int repeatCount, int evidenceCount) {
        int level = Math.max(1, Math.min(5, importance));
        double weight = 0.35 * (level / 5.0)
                + 0.25 * Math.min(Math.max(keywordCount, 0), 3) / 3.0
                + 0.20 * Math.min(Math.max(exampleCount, 0), 2) / 2.0
                + 0.10 * Math.min(Math.max(evidenceCount, 0), 5) / 5.0
                + 0.10 * Math.min(Math.max(repeatCount, 0), 2) / 2.0;
        return Math.min(1.0, Math.round(weight * 100) / 100.0);
    }
}
