package com.leadreach.crm.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把异常统一收敛成 R 结构，避免直接抛 500 页面
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务校验失败：按提示信息返回，HTTP 状态保持 200（与 user-service 一致） */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 参数/类型错误：多为前端传参问题，同样返回 R 结构便于前端提示 */
    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数异常: {}", e.getMessage());
        return R.fail(e.getMessage());
    }

    /** 兜底：未预期异常记日志，对外只给一句通用提示 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(500, "系统异常，请稍后重试");
    }
}