package com.studyloop.content.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class KnowledgeNameNormalizerTest {

    @Test
    @DisplayName("空白与连接词不影响归一化")
    void connectorsIgnored() {
        assertEquals(KnowledgeNameNormalizer.key("进程与线程"), KnowledgeNameNormalizer.key("进程 和 线程"));
        assertEquals(KnowledgeNameNormalizer.key("进程的调度"), KnowledgeNameNormalizer.key("进程调度"));
    }

    @Test
    @DisplayName("全半角与大小写统一")
    void widthAndCase() {
        assertEquals(KnowledgeNameNormalizer.key("ＦＣＦＳ"), KnowledgeNameNormalizer.key("fcfs"));
    }

    @Test
    @DisplayName("标点不影响归一化")
    void punctuationIgnored() {
        assertEquals(KnowledgeNameNormalizer.key("时间片轮转（RR）"), KnowledgeNameNormalizer.key("时间片轮转RR"));
    }

    @Test
    @DisplayName("不同知识点不会被误合并")
    void differentStayDifferent() {
        assertNotEquals(KnowledgeNameNormalizer.key("进程"), KnowledgeNameNormalizer.key("线程"));
        assertNotEquals(KnowledgeNameNormalizer.key("进程"), KnowledgeNameNormalizer.key("进程与线程"));
    }

    @Test
    @DisplayName("空值安全")
    void nullSafe() {
        assertEquals("", KnowledgeNameNormalizer.key(null));
    }
}
