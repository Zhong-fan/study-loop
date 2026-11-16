package com.studyloop.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新建 / 修改章节都用这一个。
 * <p>
 * 字段少、校验规则完全一样，硬拆成两个类只会多出一个空壳；
 * 等哪天"改名"和"改排序"需要不同权限时再拆。
 */
@Data
public class ChapterRequest {

    @NotBlank(message = "章节名称不能为空")
    @Size(max = 50, message = "章节名称不能超过 50 个字")
    private String title;

    /** 为空时排到最后 */
    private Integer sort;
}
