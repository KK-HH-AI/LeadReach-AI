package com.leadreach.user.context;

/**
 * 租户上下文：ThreadLocal 持有当前请求所属的公司ID（tenantId）
 * 由 TenantInterceptor 从请求头 X-Tenant-Id 读入，请求结束后清除
 */
public class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
