package com.leadreach.user.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leadreach.user.entity.BossEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BossEmployeeMapper extends BaseMapper<BossEmployee> {

    /**
     * 按员工ID查在职归属关系，忽略租户过滤。
     * 为什么必须忽略：登录是公开路由（无 auth 中间件），请求头里没有 X-Tenant-Id，
     * 租户插件会把条件拼成 tenant_id = NULL 而查不到任何行，
     * 导致 JWT 里拿不到 position/teamId。此处员工ID全局唯一，按ID查是安全的。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM boss_employee WHERE employee_id = #{employeeId} AND status = 1 LIMIT 1")
    BossEmployee selectActiveByEmployeeIdWithoutTenant(@Param("employeeId") Long employeeId);
}