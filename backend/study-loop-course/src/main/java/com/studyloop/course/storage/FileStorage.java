package com.studyloop.course.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 文件存储。
 * <p>
 * 现在只有本地磁盘一种实现；以后换成 MinIO 时，
 * 只要再写一个实现类并改一下注入的 Bean，业务代码不用动。
 */
public interface FileStorage {

    /**
     * 保存文件，返回存储层用的 key（不要直接暴露给前端）。
     */
    String save(MultipartFile file);

    /**
     * 按 key 找到文件路径，用于下载。
     */
    Path load(String key);

    /**
     * 删除文件。软删除的场景暂时用不到，但接口先留着。
     */
    void delete(String key);
}
