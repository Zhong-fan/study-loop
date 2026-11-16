package com.studyloop.course.storage;

import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * MinIO 存储。
 * <p>
 * 只有配置 app.storage=minio 时才会创建这个 Bean，
 * 默认走本地磁盘，所以没装 MinIO 也能把项目跑起来。
 * <p>
 * 注意 key 的格式和本地实现保持一致（日期/uuid.后缀），
 * 这样两种存储之间迁移数据时不用改数据库里的 file_key。
 */
@Component
@ConditionalOnProperty(name = "app.storage", havingValue = "minio")
public class MinioFileStorage implements FileStorage {

    private final MinioClient client;
    private final String bucket;

    public MinioFileStorage(@Value("${app.minio.endpoint}") String endpoint,
                            @Value("${app.minio.access-key}") String accessKey,
                            @Value("${app.minio.secret-key}") String secretKey,
                            @Value("${app.minio.bucket}") String bucket) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.bucket = bucket;
        ensureBucket();
    }

    @Override
    public String save(MultipartFile file) {
        String extension = LocalFileStorage.getExtension(file.getOriginalFilename());
        String key = LocalDate.now() + "/" + UUID.randomUUID() + "." + extension;

        try (InputStream in = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return key;
        } catch (Exception e) {
            throw new BizException(ErrorCode.BUSINESS_ERROR, "文件上传到 MinIO 失败");
        }
    }

    @Override
    public InputStream open(String key) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception e) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception ignored) {
            // 删不掉不影响业务
        }
    }

    /** 桶不存在就建一个，省得部署时手工点控制台 */
    private void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("无法连接 MinIO，请检查 app.minio 配置", e);
        }
    }
}
