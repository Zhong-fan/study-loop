package com.studyloop.content.support;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 真题题干归一化，用于导入去重。
 * <p>
 * 只做「不影响语义」的归一：全半角、空白、标点、大小写。
 * 不像知识点那样去连接词，避免把两道不同的题误合并。
 */
public final class ExamStemNormalizer {

    private ExamStemNormalizer() {
    }

    public static String key(String stem) {
        if (stem == null) {
            return "";
        }
        String text = Normalizer.normalize(stem, Normalizer.Form.NFKC);
        text = text.replaceAll("\\s+", "");
        text = text.replaceAll("[,，.。;；:：·\\-—()（）\\[\\]【】?？!！\"'“”‘’]", "");
        text = text.toLowerCase(Locale.ROOT);
        return text.length() > 180 ? text.substring(0, 180) : text;
    }
}
