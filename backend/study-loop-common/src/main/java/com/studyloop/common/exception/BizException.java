package com.studyloop.common.exception;

import com.studyloop.common.result.ErrorCode;

/**
 * 业务异常。
 * <p>
 * 业务代码里遇到"用户不存在""课程名重复"这类情况，直接 throw 这个异常，
 * 由 GlobalExceptionHandler 统一转成 Result 返回，不要在每个接口里写 if-else 返回错误。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
