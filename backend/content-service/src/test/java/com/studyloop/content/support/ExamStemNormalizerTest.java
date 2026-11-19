package com.studyloop.content.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ExamStemNormalizerTest {

    @Test
    @DisplayName("空白、标点、大小写不影响去重键")
    void punctuationIgnored() {
        assertEquals(ExamStemNormalizer.key("进程状态转换中，等待 I/O 完成后进入（ ）"),
                ExamStemNormalizer.key("进程状态转换中 等待 I/O 完成后进入()"));
    }

    @Test
    @DisplayName("不同题目不会被误合并")
    void differentStayDifferent() {
        assertNotEquals(ExamStemNormalizer.key("进程是什么"), ExamStemNormalizer.key("线程是什么"));
    }

    @Test
    @DisplayName("超长题干截断到 180 字")
    void truncate() {
        assertEquals(180, ExamStemNormalizer.key("题".repeat(300)).length());
    }

    @Test
    @DisplayName("空值安全")
    void nullSafe() {
        assertEquals("", ExamStemNormalizer.key(null));
    }
}
