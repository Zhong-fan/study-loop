package com.studyloop.content.vo;

import java.io.InputStream;

/**
 * 下载用的文件信息。
 * <p>
 * record 是 Java 14 之后的简化写法，等价于一个只有 final 字段和 getter 的类，
 * 适合这种纯数据的场景。
 */
public record DownloadFile(String fileName, long size, InputStream stream) {
}
