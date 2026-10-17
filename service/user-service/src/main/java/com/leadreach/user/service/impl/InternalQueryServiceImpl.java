package com.leadreach.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leadreach.user.dto.TeamDTO;
import com.leadreach.user.dto.UserDTO;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.Team;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.TeamMapper;
import com.leadreach.user.mapper.UserMapper;
import com.leadreach.user.service.InternalQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 内部查询业务实现：仅服务间调用，要求请求头 X-Internal-Call: true
 * 所有查询限定在调用方租户内，防止跨租户数据泄露。
 */
@Service
public class InternalQueryServiceImpl implements InternalQueryService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BossEmployeeMapper bossEmployeeMapper;

    @Autowired
    private TeamMapper teamMapper;

    // 查单个用户
    @Override
    public Map<String, Object> getUser(Long tenantId, Long id) {
        if (tenantId == null) {
            return tenantErr();
        }
        User user = userMapper.selectById(id);
        if (user == null || !tenantId.equals(user.getTenantId())) {
            return err("用户不存在");
        }
        List<UserDTO> dtos = toUserDTOs(List.of(user));
        return ok(dtos.get(0));
    }

    // 批量查用户：ids 为逗号分隔的用户ID串
    @Override
    public Map<String, Object> listUsers(Long tenantId, String ids) {
        if (tenantId == null) {
            return tenantErr();
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

    // 查团队下所有用户（含团队负责人）
    @Override
    public Map<String, Object> listUsersByTeam(Long tenantId, Long teamId) {
        if (tenantId == null) {
            return tenantErr();
        }
        Team team = teamMapper.selectById(teamId);
        if (team == null || !tenantId.equals(team.getTenantId())) {
            return err("团队不存在");
        }
        return ok(toUserDTOs(loadTeamUsers(team)));
    }

    // 查团队
    @Override
    public Map<String, Object> getTeam(Long tenantId, Long id) {
        if (tenantId == null) {
            return tenantErr();
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
    @Override
    public Map<String, Object> listTeamMembers(Long tenantId, Long id) {
        if (tenantId == null) {
            return tenantErr();
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
            // 姓名 V4 起有真实值，未填时回退手机号，前端展示口径不变
            dto.setName(u.getName() != null && !u.getName().isBlank() ? u.getName() : u.getPhone());
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

    /** 租户上下文缺失时的统一错误响应 */
    private Map<String, Object> tenantErr() {
        return err("缺少租户上下文");
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
