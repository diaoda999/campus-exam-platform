package com.campus.exam.admin.config;

import com.campus.exam.common.exception.BizException;
import com.campus.exam.common.result.Result;
import com.campus.exam.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class,
            IllegalArgumentException.class})
    public Result<Void> handleParam(Exception e) {
        String msg = e.getMessage();
        if (e instanceof MethodArgumentNotValidException manv) {
            FieldError fieldError = manv.getBindingResult().getFieldError();
            msg = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        }
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.SERVER_ERROR.getCode(), "服务繁忙：" + e.getMessage());
    }
}
