package com.studyloop.course.service;

import com.studyloop.course.vo.MaterialVO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * 资料业务接口。
 */
public interface MaterialService {

    Long upload(Long courseId, Long chapterId, String title, MultipartFile file);

    List<MaterialVO> listByCourse(Long courseId);

    void delete(Long id);

    /** 下载时用：先校验归属，再返回磁盘路径 */
    Path loadPath(Long id);
}
