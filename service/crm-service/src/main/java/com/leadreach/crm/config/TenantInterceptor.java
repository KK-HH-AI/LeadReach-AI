package com.leadreach.crm.config;

import com.leadreach.crm.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 身份拦截器：从网关透传的请求头读取当前用户身份，写入 UserContext
 * 供 MyBatis-Plus 租户插件与数据权限拼接使用
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.set(new UserContext.Snapshot(
                parseLong(request.getHeader("X-User-Id")),
                parseLong(request.getHeader("X-Tenant-Id")),
                parseLong(request.getHeader("X-Team-Id")),
                trimToNull(request.getHeader("X-Job-Code")),
                trimToNull(request.getHeader("X-User-Role"))
        ));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private Long parseLong(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String trimToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}