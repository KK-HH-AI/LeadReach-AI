package com.leadreach.user.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 内部接口鉴权拦截器：只放行带 X-Internal-Call: true 的请求
 *
 * 网关只路由 /api/v1/**，/internal/** 天然不对外暴露；
 * 这一层是第二道闸：即使内网被直连，没有内部调用标记也拿不到数据。
 */
@Component
public class InternalCallInterceptor implements HandlerInterceptor {

    /** 服务间调用的标记头 */
    public static final String INTERNAL_CALL_HEADER = "X-Internal-Call";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("true".equalsIgnoreCase(request.getHeader(INTERNAL_CALL_HEADER))) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(
                "{\"code\":403,\"msg\":\"内部接口，禁止直接访问\"}".getBytes(StandardCharsets.UTF_8));
        return false;
    }
}