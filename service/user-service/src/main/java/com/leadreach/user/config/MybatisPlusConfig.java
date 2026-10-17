package com.leadreach.user.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.leadreach.user.context.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置：核心是多租户插件，实现"表级隔离"
 * 原理：自动给所有 SQL 追加 AND tenant_id = 当前租户，防止开发者漏写租户条件
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {

            /**
             * 返回当前请求的租户ID。无租户上下文时返回 null 值，拼出的条件查不到任何行，保证隔离安全。
             */
            @Override
            public Expression getTenantId() {
                Long tenantId = TenantContext.getTenantId();
                return tenantId != null ? new LongValue(tenantId) : new NullValue();
            }

            /**
             * 忽略租户过滤的表：
             *  - user：跨租户共享（注册/登录/拉人按手机号查人，此时还没有租户上下文）
             *  - company：租户根表，公司ID即租户ID，不能自己过滤自己
             *  - join_request：天然跨租户（写入方是尚无租户的员工，读取方是有租户的老板），
             *    条件由 Service 显式控制
             * 注意：实体 @TableName("`user`") 带反引号，插件传入的表名可能带反引号，
             *      必须先去掉再比较，否则匹配不上会把 user 表也强制加租户条件
             */
            @Override
            public boolean ignoreTable(String tableName) {
                String t = tableName.toLowerCase().replace("`", "");
                return "user".equals(t) || "company".equals(t) || "join_request".equals(t);
            }
        }));
        return interceptor;
    }
}
