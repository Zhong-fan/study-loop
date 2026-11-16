package com.studyloop.course.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储。
 * <p>
 * 接口里返回的是 InputStream 而不是本地路径，这是关键：
 * 一旦返回 Path，实现类就被钉死在"文件必须在本地磁盘"上，
 * 换成 MinIO 时整个下载链路都要改。
 * <p>
 * 现在有两个实现，靠配置 app.storage 切换：
 * local（默认，存本地磁盘）/ minio（存对象存储）。
 */
public interface FileStorage {

    /** 保存文件，返回存储层用的 key（不暴露给前端） */
    String save(MultipartFile file);

    /** 按 key 打开文件流，用于下载 */
    InputStream open(String key);

    /** 删除文件 */
    void delete(String key);
}
