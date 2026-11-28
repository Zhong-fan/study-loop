package com.studyloop.practice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 导入时某一行的错误，行号与 Excel 可见行号一致 */
@Data
@AllArgsConstructor
public class ImportErrorVO {

    private int row;

    private String stem;

    private String message;
}
