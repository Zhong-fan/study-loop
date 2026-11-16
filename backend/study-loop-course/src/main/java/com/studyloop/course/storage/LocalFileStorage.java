package com.studyloop.course.storage;

import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 本地磁盘存储，默认实现。
 * <p>
 * 文件放在 uploads/日期/uuid.后缀。用 uuid 而不是原文件名，
 * 既避免重名覆盖，也避免有人用 ../../ 这种文件名跳出目录。
 */
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    @Override
    public String save(MultipartFile file) {
        String extension = getExtension(file.getOriginalFilename());
        String key = LocalDate.now() + "/" + UUID.randomUUID() + "." + extension;

        try {
            Path target = safePath(key);
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
            return key;
        } catch (IOException e) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "文件保存失败");
        }
    }

    @Override
    public InputStream open(String key) {
        Path path = safePath(key);
        if (!Files.exists(path)) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件读取失败");
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(safePath(key));
        } catch (IOException ignored) {
            // 删不掉不影响业务，之后可以再清理
        }
    }

    /** 拼出绝对路径，并确认没有跳出根目录 */
    private Path safePath(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "非法的文件路径");
        }
        return path;
    }

    /** 取最后一个点后面的内容，没有后缀按 bin 处理 */
    public static String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "bin";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
