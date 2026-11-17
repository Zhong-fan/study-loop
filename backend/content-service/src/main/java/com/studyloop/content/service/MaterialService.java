package com.studyloop.content.service;

import com.studyloop.content.vo.DownloadFile;
import com.studyloop.content.vo.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 资料业务接口。
 */
public interface MaterialService {

    Long upload(Long courseId, Long chapterId, String title, MultipartFile file);

    List<MaterialVO> listByCourse(Long courseId);

    void delete(Long id);

    /** 下载用：先校验归属，再打开文件流（本地磁盘和 MinIO 都走这里） */
    DownloadFile download(Long id);
}
