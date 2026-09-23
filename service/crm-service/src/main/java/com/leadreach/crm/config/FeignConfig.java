package com.leadreach.crm.config;

import com.leadreach.crm.context.UserContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置：调 user-service 内部接口时统一带上身份头
 *
 * 1. X-Internal-Call: true —— user-service 的内部接口鉴权要求
 * 2. X-Tenant-Id —— 让 user-service 的租户插件按 crm 的租户过滤，
 *    避免 A 公司查到 B 公司的人员数据
 * 3. X-Job-Code / X-Team-Id —— 部分内部接口（如可分配人员）需要按调用者岗位收窄范围
 *
 * Feign 调用发生在当前请求线程内，所以能直接读到 UserContext。
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalCallInterceptor() {
        return template -> {
            template.header("X-Internal-Call", "true");
            Long tenantId = UserContext.getTenantId();
            if (tenantId != null) {
                template.header("X-Tenant-Id", String.valueOf(tenantId));
            }
            Long teamId = UserContext.getTeamId();
            if (teamId != null) {
                template.header("X-Team-Id", String.valueOf(teamId));
            }
            String jobCode = UserContext.getJobCode();
            if (jobCode != null) {
                template.header("X-Job-Code", jobCode);
            }
        };
    }
}