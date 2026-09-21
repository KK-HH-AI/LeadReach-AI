package com.leadreach.user.config;

import com.leadreach.user.context.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户拦截器：从网关透传的 X-Tenant-Id 请求头读取租户ID，写入 TenantContext，
 * 供 MyBatis-Plus 多租户插件在 SQL 层面自动做租户隔离。请求结束清除，防止线程复用串租户。
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(Long.parseLong(tenantId.trim()));
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
