package com.leadreach.crm.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.leadreach.crm.context.UserContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 *
 * 插件顺序（MyBatis-Plus 官方要求）：多租户 → 分页
 * 1. TenantLineInnerInterceptor：自动给所有 SQL 追加 AND tenant_id = 当前租户，
 *    crm_db 的业务表全部带 tenant_id，所以不加任何 ignore 表
 * 2. PaginationInnerInterceptor：分页查询必需，否则 selectPage 不返回 total
 *
 * 数据权限（SELF/TEAM/ALL）不在这里做，而是由 common/DataScope 在列表查询上显式拼接：
 * 线索的「详情」「领取」「分配」等场景不能带 owner 过滤（否则公共池线索查不到、领取会被自己挡住），
 * 用全局拦截器容易误伤，显式拼接更可控。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {

            /**
             * 当前租户ID。没有租户上下文时返回 NULL 值，条件变成 tenant_id = NULL，
             * 查不到任何数据——这是安全的兜底，避免漏掉租户条件时跨租户泄露。
             */
            @Override
            public Expression getTenantId() {
                Long tenantId = UserContext.getTenantId();
                return tenantId != null ? new LongValue(tenantId) : new NullValue();
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // crm_db 里所有业务表都带 tenant_id，没有需要忽略的表
                return false;
            }
        }));

        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}