package com.leadreach.user.controller;

import com.leadreach.user.context.TenantContext;
import com.leadreach.user.service.InternalQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 内部接口：仅服务间调用，要求请求头 X-Internal-Call: true
 * 网关不路由 /internal/**，外部无法触达。
 *
 * 只做参数接收与租户上下文传递（X-Tenant-Id 经 TenantInterceptor 写入 TenantContext），
 * 查询逻辑在 InternalQueryService（service 包），所有查询限定在调用方租户内。
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    @Autowired
    private InternalQueryService internalQueryService;

    // 查单个用户
    @GetMapping("/users/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        return internalQueryService.getUser(TenantContext.getTenantId(), id);
    }

    // 批量查用户：GET /internal/users?ids=1,2,3
    @GetMapping("/users")
    public Map<String, Object> listUsers(@RequestParam(value = "ids", required = false) String ids) {
        return internalQueryService.listUsers(TenantContext.getTenantId(), ids);
    }

    // 查团队下所有用户（含团队负责人）
    @GetMapping("/users/by-team")
    public Map<String, Object> listUsersByTeam(@RequestParam("teamId") Long teamId) {
        return internalQueryService.listUsersByTeam(TenantContext.getTenantId(), teamId);
    }

    // 查团队
    @GetMapping("/teams/{id}")
    public Map<String, Object> getTeam(@PathVariable Long id) {
        return internalQueryService.getTeam(TenantContext.getTenantId(), id);
    }

    // 查团队成员列表
    @GetMapping("/teams/{id}/members")
    public Map<String, Object> listTeamMembers(@PathVariable Long id) {
        return internalQueryService.listTeamMembers(TenantContext.getTenantId(), id);
    }
}
