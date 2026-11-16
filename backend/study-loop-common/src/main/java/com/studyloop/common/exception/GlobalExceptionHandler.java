package com.studyloop.common.exception;

import com.studyloop.common.result.ErrorCode;
import com.studyloop.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 * <p>
 * 作用只有一个：让所有接口在出错时也返回 Result 结构，
 * 而不是把 Java 异常堆栈直接抛给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：可预期，按业务码原样返回 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** 参数校验失败：@NotBlank 之类没通过 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = ErrorCode.PARAM_ERROR.getMessage();
        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            message = fieldError.getDefaultMessage();
        }
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /** 兜底：没预料到的异常，记录日志，对外只说系统繁忙 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未处理的异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage());
    }
}
