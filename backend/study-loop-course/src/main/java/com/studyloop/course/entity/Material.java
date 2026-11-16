package com.studyloop.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程资料（课件文件）。
 * <p>
 * 数据库里只存文件的"钥匙"（fileKey），文件本体放在磁盘或对象存储里。
 * 这样换存储方式时只需要改 FileStorage 的实现，表和业务代码都不用动。
 */
@Data
@TableName("material")
public class Material {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long courseId;

    /** 可以没有章节，表示"还没归类" */
    private Long chapterId;

    private String title;

    /** 用户上传时的原始文件名，下载时还原给它 */
    private String fileName;

    /** 存储层的路径，不是给前端用的 */
    private String fileKey;

    private Long fileSize;

    private String fileType;

    private LocalDateTime createdAt;

    private Integer deleted;
}
