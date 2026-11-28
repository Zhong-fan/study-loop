package com.studyloop.practice.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 题库导入结果；默认整批导入，任何一行错误都不落库 */
@Data
public class ImportResultVO {

    private int total;

    private int success;

    private int failed;

    /** true 表示因为存在错误，整批没有写入 */
    private boolean aborted;

    private List<ImportErrorVO> errors = new ArrayList<>();

    private List<Long> createdIds = new ArrayList<>();

    public static ImportResultVO aborted(int total, List<ImportErrorVO> errors) {
        ImportResultVO result = new ImportResultVO();
        result.setTotal(total);
        result.setFailed(errors.size());
        result.setAborted(true);
        result.setErrors(errors);
        return result;
    }

    public static ImportResultVO done(int total, int success, List<ImportErrorVO> errors, List<Long> createdIds) {
        ImportResultVO result = new ImportResultVO();
        result.setTotal(total);
        result.setSuccess(success);
        result.setFailed(errors.size());
        result.setAborted(false);
        result.setErrors(errors);
        result.setCreatedIds(createdIds);
        return result;
    }
}
