package com.leadreach.user.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册租户拦截器（全局）与内部接口鉴权拦截器（仅 /internal/**）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Autowired
    private InternalCallInterceptor internalCallInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/**");
        registry.addInterceptor(internalCallInterceptor).addPathPatterns("/internal/**");
    }
}