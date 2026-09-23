package com.leadreach.crm.context;

/**
 * 用户上下文：ThreadLocal 持有当前请求的身份信息
 *
 * 数据全部来自网关透传的请求头（网关已校验 JWT，下游不再解析 Token）：
 *   X-User-Id   → userId
 *   X-Tenant-Id → tenantId（= company.id）
 *   X-Team-Id   → teamId
 *   X-Job-Code  → jobCode（BOSS / SALES_MANAGER / SALES_REP / OPERATOR）
 *   X-User-Role → role（BOSS / EMPLOYEE）
 *
 * 由 TenantInterceptor 写入，afterCompletion 清除，防止线程复用串数据。
 */
public class UserContext {

    /** 一次请求的身份快照 */
    public record Snapshot(Long userId, Long tenantId, Long teamId, String jobCode, String role) {
    }

    private static final ThreadLocal<Snapshot> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Snapshot snapshot) {
        HOLDER.set(snapshot);
    }

    private static Snapshot get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        return get() == null ? null : get().userId();
    }

    public static Long getTenantId() {
        return get() == null ? null : get().tenantId();
    }

    public static Long getTeamId() {
        return get() == null ? null : get().teamId();
    }

    public static String getJobCode() {
        return get() == null ? null : get().jobCode();
    }

    public static String getRole() {
        return get() == null ? null : get().role();
    }

    public static void clear() {
        HOLDER.remove();
    }
}