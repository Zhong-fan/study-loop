package com.studyloop.content.support;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 知识点名称归一化。
 * <p>
 * 目标：把「进程与线程」「进程 和 线程」「进程线程」当成同一个键，
 * 但不做过度合并——「进程」和「进程与线程」仍然是两个知识点。
 * <p>
 * 规则：全半角统一 → 去空白 → 去连接词（的/与/和/及/、）→ 去标点 → 小写。
 */
public final class KnowledgeNameNormalizer {

    private KnowledgeNameNormalizer() {
    }

    public static String key(String name) {
        if (name == null) {
            return "";
        }
        String text = Normalizer.normalize(name, Normalizer.Form.NFKC);
        text = text.replaceAll("\\s+", "");
        text = text.replaceAll("[的与和及、]", "");
        text = text.replaceAll("[,，.。;；:：·\\-—()（）\\[\\]【】]", "");
        return text.toLowerCase(Locale.ROOT);
    }
}
