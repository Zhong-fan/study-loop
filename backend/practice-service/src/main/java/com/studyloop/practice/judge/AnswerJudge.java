package com.studyloop.practice.judge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 客观题判分。
 * <p>
 * 只做一件事：给定标准答案和用户作答，算出得分。不碰数据库、不碰 Spring，
 * 所以这段逻辑最容易写单元测试，也最容易在答辩时现场改动演示。
 * <p>
 * 得分约定：1.0 全对，0.5 部分对（只有多选漏选会给），0 错。
 */
public class AnswerJudge {

    private AnswerJudge() {
    }

    /** 这几种题型能自动判分，主观题和图片题只能自评 */
    public static boolean autoGradable(String type) {
        return "SINGLE".equals(type)
                || "MULTIPLE".equals(type)
                || "JUDGE".equals(type)
                || "BLANK".equals(type);
    }

    public static double judge(String type, List<String> correct, List<String> submitted) {
        if (correct == null || correct.isEmpty() || submitted == null || submitted.isEmpty()) {
            return 0;
        }
        if ("SINGLE".equals(type) || "JUDGE".equals(type)) {
            return normalize(correct.get(0)).equals(normalize(submitted.get(0))) ? 1.0 : 0;
        }
        if ("MULTIPLE".equals(type)) {
            return judgeMultiple(correct, submitted);
        }
        if ("BLANK".equals(type)) {
            return judgeBlank(correct, submitted);
        }
        return 0;
    }

    /**
     * 多选：全对 1.0；只漏选（作答是答案的真子集）= 0.5；只要选错一个就是 0。
     * 和很多考试的多选给分规则一致。
     */
    private static double judgeMultiple(List<String> correct, List<String> submitted) {
        Set<String> correctSet = new LinkedHashSet<>();
        for (String item : correct) {
            correctSet.add(normalize(item));
        }
        Set<String> submittedSet = new LinkedHashSet<>();
        for (String item : submitted) {
            submittedSet.add(normalize(item));
        }

        // 有任何一个不在标准答案里，直接 0 分
        for (String item : submittedSet) {
            if (!correctSet.contains(item)) {
                return 0;
            }
        }
        if (submittedSet.size() == correctSet.size()) {
            return 1.0;
        }
        return 0.5;
    }

    /** 填空：任何一个可接受答案匹配就算对 */
    private static double judgeBlank(List<String> correct, List<String> submitted) {
        String userAnswer = normalize(submitted.get(0));
        List<String> normalized = new ArrayList<>();
        for (String item : correct) {
            normalized.add(normalize(item));
        }
        return normalized.contains(userAnswer) ? 1.0 : 0;
    }

    /** 去首尾空格、去中间空格、全角空格、忽略大小写 */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replace(" ", "")
                .replace("　", "")
                .toLowerCase();
    }
}
