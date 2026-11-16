package com.studyloop.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.common.context.UserContext;
import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;
import com.studyloop.course.entity.Course;
import com.studyloop.course.entity.Material;
import com.studyloop.course.mapper.CourseMapper;
import com.studyloop.course.mapper.MaterialMapper;
import com.studyloop.course.service.MaterialPageService;
import com.studyloop.course.service.MaterialService;
import com.studyloop.course.storage.FileStorage;
import com.studyloop.course.storage.LocalFileStorage;
import com.studyloop.course.vo.DownloadFile;
import com.studyloop.course.vo.MaterialVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 资料业务实现。
 * <p>
 * 上传顺序：先校验课程归属 → 校验后缀 → 存文件 → 写数据库。
 * 反过来的话，文件存失败会在数据库里留下一条打不开的记录。
 */
@Slf4j
@Service
public class MaterialServiceImpl implements MaterialService {

    /** 允许上传的后缀，和前端 accept 保持一致 */
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md", "png", "jpg", "jpeg", "zip"
    );

    private final MaterialMapper materialMapper;
    private final CourseMapper courseMapper;
    private final FileStorage fileStorage;
    private final MaterialPageService pageService;

    public MaterialServiceImpl(MaterialMapper materialMapper,
                               CourseMapper courseMapper,
                               FileStorage fileStorage,
                               MaterialPageService pageService) {
        this.materialMapper = materialMapper;
        this.courseMapper = courseMapper;
        this.fileStorage = fileStorage;
        this.pageService = pageService;
    }

    @Override
    public Long upload(Long courseId, Long chapterId, String title, MultipartFile file) {
        Course course = checkCourse(courseId);

        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename() == null ? "未命名" : file.getOriginalFilename();
        String extension = LocalFileStorage.getExtension(originalName);
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的文件类型：" + extension);
        }

        String fileKey = fileStorage.save(file);

        Material material = new Material();
        material.setUserId(course.getUserId());
        material.setCourseId(courseId);
        material.setChapterId(chapterId);
        material.setTitle(title == null || title.isBlank() ? removeExtension(originalName) : title.trim());
        material.setFileName(originalName);
        material.setFileKey(fileKey);
        material.setFileSize(file.getSize());
        material.setFileType(extension);
        material.setCreatedAt(LocalDateTime.now());
        material.setDeleted(0);

        materialMapper.insert(material);

        // PDF 顺手抽一次文字；解析失败不影响上传结果，只记日志
        if ("pdf".equals(extension)) {
            try (InputStream in = fileStorage.open(fileKey)) {
                pageService.parsePdf(material.getId(), course.getUserId(), in);
            } catch (Exception e) {
                log.warn("资料解析失败，materialId={}", material.getId(), e);
            }
        }

        return material.getId();
    }

    @Override
    public List<MaterialVO> listByCourse(Long courseId) {
        checkCourse(courseId);

        QueryWrapper<Material> wrapper = new QueryWrapper<>();
        wrapper.eq("course_id", courseId);
        wrapper.eq("deleted", 0);
        wrapper.orderByDesc("created_at");

        List<Material> materials = materialMapper.selectList(wrapper);
        List<MaterialVO> result = new ArrayList<>();
        for (Material material : materials) {
            result.add(toVO(material));
        }
        return result;
    }

    @Override
    public void delete(Long id) {
        Material material = findOwnedMaterial(id);
        material.setDeleted(1);
        materialMapper.updateById(material);
        // 先不删磁盘文件：万一是误删还能恢复；磁盘清理以后单独做
    }

    @Override
    public DownloadFile download(Long id) {
        Material material = findOwnedMaterial(id);
        return new DownloadFile(
                material.getFileName(),
                material.getFileSize() == null ? 0L : material.getFileSize(),
                fileStorage.open(material.getFileKey())
        );
    }

    private Course checkCourse(Long courseId) {
        Long userId = UserContext.requireUserId();

        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getDeleted() == 1 || !userId.equals(course.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private Material findOwnedMaterial(Long id) {
        Long userId = UserContext.requireUserId();

        Material material = materialMapper.selectById(id);
        if (material == null || material.getDeleted() == 1 || !userId.equals(material.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "资料不存在");
        }
        return material;
    }

    private String removeExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private MaterialVO toVO(Material material) {
        MaterialVO vo = new MaterialVO();
        vo.setId(material.getId());
        vo.setCourseId(material.getCourseId());
        vo.setChapterId(material.getChapterId());
        vo.setTitle(material.getTitle());
        vo.setFileName(material.getFileName());
        vo.setFileSize(material.getFileSize());
        vo.setFileType(material.getFileType());
        vo.setDownloadUrl("/api/materials/" + material.getId() + "/download");
        vo.setCreatedAt(material.getCreatedAt());
        return vo;
    }
}
