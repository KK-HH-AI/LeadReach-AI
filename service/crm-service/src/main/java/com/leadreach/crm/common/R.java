package com.leadreach.crm.common;

import lombok.Data;

/**
 * 统一响应体：与 user-service 保持一致（code/msg/data），前端拦截器按 code===0 判成功
 */
@Data
public class R<T> {

    /** 业务码：0成功，非0失败 */
    private Integer code;
    /** 提示信息 */
    private String msg;
    /** 业务数据 */
    private T data;

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(0);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.setCode(1);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(Integer code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}