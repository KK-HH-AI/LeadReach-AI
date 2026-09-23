package com.leadreach.crm.common;

/**
 * 业务异常：service 层校验不通过时抛出，由 GlobalExceptionHandler 统一转成 R.fail
 */
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String msg) {
        super(msg);
        this.code = 1;
    }

    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}