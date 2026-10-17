package com.leadreach.user.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leadreach.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 老板拉人检索：按 姓名/邮箱/手机 模糊匹配员工，忽略租户过滤。
     * 为什么必须忽略：要检索的自由账号 tenant_id 为 NULL，
     * 租户插件会把条件拼成 tenant_id = 当前租户，自由账号永远查不出来。
     * 安全边界：SQL 内限定 tenant_id IS NULL OR tenant_id = 当前租户，
     * 不会泄露其他公司成员信息。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM `user` WHERE role = 'EMPLOYEE' "
            + "AND (name LIKE CONCAT('%', #{keyword}, '%') "
            + "OR email LIKE CONCAT('%', #{keyword}, '%') "
            + "OR phone LIKE CONCAT('%', #{keyword}, '%')) "
            + "AND (tenant_id IS NULL OR tenant_id = #{tenantId}) "
            + "ORDER BY id DESC LIMIT 20")
    List<User> searchEmployeesByKeyword(@Param("keyword") String keyword, @Param("tenantId") Long tenantId);
}
