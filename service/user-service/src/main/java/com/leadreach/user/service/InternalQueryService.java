package com.leadreach.user.service;

import java.util.Map;

/**
 * 内部查询业务接口：仅服务间调用，网关不路由 /internal/**
 *
 * 租户范围：tenantId 由 Controller 从 TenantContext（X-Tenant-Id 头）取值传入，
 * 所有查询都限定在该租户内，防止 A 公司拿到 B 公司的人员/团队数据。
 */
public interface InternalQueryService {

    /** 查单个用户（限定本租户） */
    Map<String, Object> getUser(Long tenantId, Long id);

    /** 批量查用户：ids 为逗号分隔的用户ID串（限定本租户） */
    Map<String, Object> listUsers(Long tenantId, String ids);

    /** 查团队下所有用户（含团队负责人） */
    Map<String, Object> listUsersByTeam(Long tenantId, Long teamId);

    /** 查团队（含负责人手机号） */
    Map<String, Object> getTeam(Long tenantId, Long id);

    /** 查团队成员列表 */
    Map<String, Object> listTeamMembers(Long tenantId, Long id);
}
