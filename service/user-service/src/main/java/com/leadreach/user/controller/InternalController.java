package com.leadreach.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leadreach.user.context.TenantContext;
import com.leadreach.user.dto.TeamDTO;
import com.leadreach.user.dto.UserDTO;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.Team;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.TeamMapper;
import com.leadreach.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内部接口：仅服务间调用（crm-service 等），要求请求头 X-Internal-Call: true
 * 网关不路由 /internal/**，外部无法触达。
 *
 * 租户范围：调用方通过 X-Tenant-Id 带入自己的租户，本控制器的所有查询都限定在该租户内，
 * 防止 A 公司拿到 B 公司的人员/团队数据。
 */
@RestController
@RequestMapping("/internal")
public class InternalController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BossEmployeeMapper bossEmployeeMapper;

    @Autowired
    private TeamMapper teamMapper;

    // 查单个用户
    @GetMapping("/users/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        Long tenantId = currentTenantId();
        if (tenantId == null) {
            return err("缺少租户上下文");
        }
        User user = userMapper.selectById(id);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            return err("用户不存在");
        }
        List<UserDTO> dtos = toUserDTOs(List.of(user));
        return ok(dtos.get(0));
    }

    // 批量查用户：GET /internal/users?ids=1,2,3
    @GetMapping("/users")
    public Map<String, Object> listUsers(@RequestParam(value = "ids", required = false) String ids) {
        Long tenantId = currentTenantId();
        if (tenantId == null) {
            return err("缺少租户上下文");
        }
        if (ids == null || ids.isBlank()) {
            return ok(List.of());
        }
        List<Long> idList = new ArrayList<>();
        for (String s : ids.split(",")) {
            if (!s.isBlank()) {
                idList.add(Long.parseLong(s.trim()));
            }
        }
        if (idList.isEmpty()) {
            return ok(List.of());
        }
        List<User> users = userMapper.selectBatchIds(idList).stream()
                .filter(u -> tenantId.equals(u.getTenantId()))
                .toList();
        return ok(toUserDTOs(users));
    }

    /**
     * 可分配的销售专员：老板看全公司，销售主管只看本团队。
     * 线索分配的下拉数据源，岗位/团队由调用方（crm-service）从网关头透传过来。
     */
    @GetMapping("/users/assignable")
    public Map<String, Object> listAssignableUsers(
            @RequestHeader(value = "X-Job-Code", required = false) String jobCode,
            @RequestHeader(value = "X-Team-Id", required = false) Long teamId) {
        Long tenantId = currentTenantId();
        if (tenantId == null) {
            return err("缺少租户上下文");
        }
        LambdaQueryWrapper<BossEmployee> w = Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getPosition, "SALES_REP")
                .eq(BossEmployee::getStatus, 1);
        if (!"BOSS".equals(jobCode)) {
            if (teamId == null) {
                return err("您还没有被分配到具体团队");
            }
            w.eq(BossEmployee::getTeamId, teamId);
        }
        List<Long> userIds = bossEmployeeMapper.selectList(w).stream()
                .map(BossEmployee::getEmployeeId)
                .toList();
        if (userIds.isEmpty()) {
            return ok(List.of());
        }
        List<User> users = userMapper.selectBatchIds(userIds).stream()
                .filter(u -> tenantId.equals(u.getTenantId()))
                .toList();
        return ok(toUserDTOs(users));
    }

    // 查团队下所有用户（含团队负责人）
    @GetMapping("/users/by-team")
    public Map<String, Object> listUsersByTeam(@RequestParam("teamId") Long teamId) {
        Long tenantId = currentTenantId();
        if (tenantId == null) {
            return err("缺少租户上下文");
        }
        Team team = teamMapper.selectById(teamId);
        if (team == null || !tenantId.equals(team.getTenantId())) {
            return err("团队不存在");
        }
        return ok(toUserDTOs(loadTeamUsers(team)));
    }

    // 查团队
    @GetMapping("/teams/{id}")
    public Map<String, Object> getTeam(@PathVariable Long id) {
        Long tenantId = currentTenantId();
        if (tenantId == null) {
            return err("缺少租户上下文");
        }
        Team team = teamMapper.selectById(id);
        if (team == null || !tenantId.equals(team.getTenantId())) {
            return err("团队不存在");
        }
        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setTenantId(team.getTenantId());
        dto.setName(team.getName());
        dto.setLeaderUserId(team.getLeaderUserId());
        dto.setIsDefault(team.getIsDefault());
        dto.setStatus(team.getStatus());
        if (team.getLeaderUserId() != null) {
            User leader = userMapper.selectById(team.getLeaderUserId());
            dto.setLeaderPhone(leader == null ? null : leader.getPhone());
        }
        return ok(dto);
    }

    // 查团队成员列表
    @GetMapping("/teams/{id}/members")
    public Map<String, Object> listTeamMembers(@PathVariable Long id) {
        Long tenantId = currentTenantId();
        if (tenantId == null) {
            return err("缺少租户上下文");
        }
        Team team = teamMapper.selectById(id);
        if (team == null || !tenantId.equals(team.getTenantId())) {
            return err("团队不存在");
        }
        return ok(toUserDTOs(loadTeamUsers(team)));
    }

    /**
     * 团队用户集合 = 团队成员(boss_employee.team_id) + 团队负责人
     * 负责人不一定在 boss_employee 里（默认团队的负责人是老板本人），所以单独补进去
     */
    private List<User> loadTeamUsers(Team team) {
        List<BossEmployee> members = bossEmployeeMapper.selectList(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getTeamId, team.getId())
                .eq(BossEmployee::getStatus, 1));
        Set<Long> userIds = new LinkedHashSet<>();
        members.forEach(m -> userIds.add(m.getEmployeeId()));
        if (team.getLeaderUserId() != null) {
            userIds.add(team.getLeaderUserId());
        }
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> userMap = new HashMap<>();
        userMapper.selectBatchIds(userIds).forEach(u -> userMap.put(u.getId(), u));
        return userIds.stream().map(userMap::get).filter(java.util.Objects::nonNull).toList();
    }

    /**
     * 组装 UserDTO：补齐岗位、团队ID、团队名
     * 岗位来源：老板固定 BOSS，员工取 boss_employee.position
     */
    private List<UserDTO> toUserDTOs(List<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(User::getId).toList();

        Map<Long, BossEmployee> relationMap = new HashMap<>();
        bossEmployeeMapper.selectList(Wrappers.<BossEmployee>lambdaQuery()
                        .in(BossEmployee::getEmployeeId, userIds)
                        .eq(BossEmployee::getStatus, 1))
                .forEach(r -> relationMap.putIfAbsent(r.getEmployeeId(), r));

        Set<Long> teamIds = new LinkedHashSet<>();
        relationMap.values().forEach(r -> {
            if (r.getTeamId() != null) {
                teamIds.add(r.getTeamId());
            }
        });
        Map<Long, String> teamNameMap = new HashMap<>();
        if (!teamIds.isEmpty()) {
            teamMapper.selectBatchIds(teamIds).forEach(t -> teamNameMap.put(t.getId(), t.getName()));
        }

        List<UserDTO> result = new ArrayList<>();
        for (User u : users) {
            UserDTO dto = new UserDTO();
            dto.setId(u.getId());
            dto.setTenantId(u.getTenantId());
            dto.setPhone(u.getPhone());
            dto.setName(u.getPhone());
            dto.setStatus(u.getStatus());
            BossEmployee relation = relationMap.get(u.getId());
            if (relation == null) {
                dto.setJobCode("BOSS".equals(u.getRole()) ? "BOSS" : null);
            } else {
                dto.setJobCode(relation.getPosition());
                dto.setTeamId(relation.getTeamId());
                dto.setTeamName(relation.getTeamId() == null ? null : teamNameMap.get(relation.getTeamId()));
            }
            result.add(dto);
        }
        return result;
    }

    /** 当前租户：由 X-Tenant-Id 经 TenantInterceptor 写入 */
    private Long currentTenantId() {
        return TenantContext.getTenantId();
    }

    private Map<String, Object> ok(Object data) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 0);
        res.put("msg", "ok");
        res.put("data", data);
        return res;
    }

    private Map<String, Object> err(String msg) {
        Map<String, Object> res = new HashMap<>();
        res.put("code", 1);
        res.put("msg", msg);
        return res;
    }
}