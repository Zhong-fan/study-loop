package com.studyloop.common.result;

/**
 * 错误码。
 * <p>
 * 分段规则：1xxx 参数问题，2xxx 权限问题，3xxx 业务问题，5xxx 系统问题。
 * 新增错误码时按段往后加，不要打乱已有编号。
 */
public enum ErrorCode {

    PARAM_ERROR(1001, "参数不合法"),
    UNAUTHORIZED(2001, "未登录或登录已过期"),
    FORBIDDEN(2002, "没有权限操作这条数据"),

    NOT_FOUND(3001, "数据不存在"),
    BUSINESS_ERROR(3002, "业务处理失败"),

    SYSTEM_ERROR(5000, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
