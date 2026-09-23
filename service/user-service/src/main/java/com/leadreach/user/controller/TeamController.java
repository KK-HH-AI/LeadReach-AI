package com.leadreach.user.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leadreach.user.context.TenantContext;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.ContentAssignment;
import com.leadreach.user.entity.Team;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.ContentAssignmentMapper;
import com.leadreach.user.mapper.TeamMapper;
import com.leadreach.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 团队业务：老板管理团队、拉人、删除人、分配/修改员工负责的内容
 *
 * 团队模型（V3 起）：公司(租户)下可建多个 team，团队由销售主管带队，
 * boss_employee.team_id 记录员工归属，是线索模块 TEAM 级数据权限的依据。
 * 公司创建后自动拥有一个「默认团队」，未显式指定团队的成员落在默认团队里。
 */
@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    /** 岗位白名单 */
    private static final Map<String, String> POSITIONS = Map.of(
            "SALES_REP", "销售专员",
            "SALES_MANAGER", "销售主管",
            "OPERATOR", "运营"
    );

    /** 默认团队名称 */
    private static final String DEFAULT_TEAM_NAME = "默认团队";

    @Autowired
    private BossEmployeeMapper bossEmployeeMapper;

    @Autowired
    private ContentAssignmentMapper contentAssignmentMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private UserMapper userMapper;

    // 创建团队：幂等确认，返回团队概况
    @PostMapping
    public Map<String, Object> create(HttpServletRequest request) {
        Map<String, Object> auth = currentBoss(request);
        if (auth == null) {
            return Map.of("code", 1, "msg", "只有已创建公司的老板才能创建团队");
        }
        Long bossId = (Long) auth.get("bossId");
        Long tenantId = (Long) auth.get("tenantId");

        long count = bossEmployeeMapper.selectCount(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId));
        Team defaultTeam = ensureDefaultTeam(bossId, tenantId);
        Map<String, Object> data = new HashMap<>();
        data.put("teamId", defaultTeam.getId());
        data.put("bossId", bossId);
        data.put("memberCount", count);
        return Map.of("code", 0, "msg", "团队可用", "data", data);
    }

    // 团队成员列表（兼容 /teams 与 /teams/ 两种路径）
    @GetMapping({"", "/"})
    public Map<String, Object> list(HttpServletRequest request) {
        Map<String, Object> auth = currentBoss(request);
        if (auth == null) {
            return Map.of("code", 1, "msg", "只有已创建公司的老板才能查看团队");
        }
        Long bossId = (Long) auth.get("bossId");
        List<BossEmployee> members = bossEmployeeMapper.selectList(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId));

        // 批量取员工手机号（避免 N+1），前端展示需要
        List<Long> empIds = members.stream().map(BossEmployee::getEmployeeId).toList();
        Map<Long, String> phoneMap = new HashMap<>();
        if (!empIds.isEmpty()) {
            userMapper.selectBatchIds(empIds).forEach(u -> phoneMap.put(u.getId(), u.getPhone()));
        }
        // 团队名映射，随成员一起下发，前端可直接展示归属团队
        Map<Long, String> teamNameMap = new HashMap<>();
        teamMapper.selectList(Wrappers.<Team>lambdaQuery().eq(Team::getTenantId, TenantContext.getTenantId()))
                .forEach(t -> teamNameMap.put(t.getId(), t.getName()));

        List<Map<String, Object>> data = new ArrayList<>();
        for (BossEmployee m : members) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", m.getEmployeeId());
            item.put("phone", phoneMap.getOrDefault(m.getEmployeeId(), ""));
            item.put("position", m.getPosition());
            item.put("teamId", m.getTeamId());
            item.put("teamName", m.getTeamId() == null ? null : teamNameMap.get(m.getTeamId()));
            item.put("createdAt", m.getCreatedAt());
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 团队列表：公司下的所有团队，含负责人与人数
    @GetMapping("/groups")
    public Map<String, Object> listGroups(HttpServletRequest request) {
        Map<String, Object> auth = currentBoss(request);
        if (auth == null) {
            return Map.of("code", 1, "msg", "只有已创建公司的老板才能查看团队");
        }
        Long tenantId = (Long) auth.get("tenantId");

        List<Team> teams = teamMapper.selectList(Wrappers.<Team>lambdaQuery()
                .eq(Team::getTenantId, tenantId)
                .orderByAsc(Team::getId));

        // 负责人手机号 + 各团队人数，一次性查出来避免 N+1
        List<Long> leaderIds = teams.stream().map(Team::getLeaderUserId).filter(java.util.Objects::nonNull).toList();
        Map<Long, String> leaderPhoneMap = new HashMap<>();
        if (!leaderIds.isEmpty()) {
            userMapper.selectBatchIds(leaderIds).forEach(u -> leaderPhoneMap.put(u.getId(), u.getPhone()));
        }
        List<BossEmployee> allMembers = bossEmployeeMapper.selectList(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getTenantId, tenantId));
        Map<Long, Long> memberCountMap = new HashMap<>();
        for (BossEmployee m : allMembers) {
            if (m.getTeamId() != null) {
                memberCountMap.merge(m.getTeamId(), 1L, Long::sum);
            }
        }

        List<Map<String, Object>> data = new ArrayList<>();
        for (Team t : teams) {
            Map<String, Object> item = new HashMap<>();
            item.put("teamId", t.getId());
            item.put("name", t.getName());
            item.put("leaderUserId", t.getLeaderUserId());
            item.put("leaderPhone", t.getLeaderUserId() == null ? null : leaderPhoneMap.get(t.getLeaderUserId()));
            item.put("isDefault", t.getIsDefault());
            item.put("memberCount", memberCountMap.getOrDefault(t.getId(), 0L));
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 创建团队：指定名称与负责人（负责人必须是本公司销售主管）
    @PostMapping("/groups")
    @Transactional
    public Map<String, Object> createGroup(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Map<String, Object> auth = currentBoss(request);
        if (auth == null) {
            return Map.of("code", 1, "msg", "只有已创建公司的老板才能创建团队");
        }
        Long bossId = (Long) auth.get("bossId");
        Long tenantId = (Long) auth.get("tenantId");

        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return Map.of("code", 1, "msg", "团队名称不能为空");
        }
        name = name.trim();
        Long exists = teamMapper.selectCount(Wrappers.<Team>lambdaQuery()
                .eq(Team::getTenantId, tenantId)
                .eq(Team::getName, name));
        if (exists != null && exists > 0) {
            return Map.of("code", 1, "msg", "同名团队已存在");
        }

        // 负责人可选：给了手机号就必须是本公司在职的销售主管
        Long leaderUserId = null;
        String leaderPhone = body.get("leaderPhone");
        if (leaderPhone != null && !leaderPhone.isBlank()) {
            User leader = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, leaderPhone.trim()));
            if (leader == null) {
                return Map.of("code", 1, "msg", "负责人手机号未注册");
            }
            BossEmployee relation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                    .eq(BossEmployee::getBossId, bossId)
                    .eq(BossEmployee::getEmployeeId, leader.getId())
                    .eq(BossEmployee::getStatus, 1));
            if (relation == null) {
                return Map.of("code", 1, "msg", "负责人不在本团队中，请先拉人入队");
            }
            if (!"SALES_MANAGER".equals(relation.getPosition())) {
                return Map.of("code", 1, "msg", "团队负责人岗位必须是销售主管");
            }
            leaderUserId = leader.getId();
        }

        Team team = new Team();
        team.setTenantId(tenantId);
        team.setName(name);
        team.setLeaderUserId(leaderUserId);
        team.setIsDefault(0);
        team.setStatus(1);
        teamMapper.insert(team);

        // 负责人自动归入自己带的团队
        if (leaderUserId != null) {
            BossEmployee relation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                    .eq(BossEmployee::getBossId, bossId)
                    .eq(BossEmployee::getEmployeeId, leaderUserId));
            relation.setTeamId(team.getId());
            bossEmployeeMapper.updateById(relation);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("teamId", team.getId());
        data.put("name", team.getName());
        return Map.of("code", 0, "msg", "创建团队成功", "data", data);
    }

    // 拉人入队：员工必须先注册；teamId 可省略，缺省落在默认团队
    @PostMapping("/members")
    @Transactional
    public Map<String, Object> addMember(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Map<String, Object> auth = currentBoss(request);
        if (auth == null) {
            return Map.of("code", 1, "msg", "只有已创建公司的老板才能拉人");
        }
        Long bossId = (Long) auth.get("bossId");
        Long tenantId = (Long) auth.get("tenantId");

        String phone = body.get("phone");
        String position = body.get("position");
        if (position == null || !POSITIONS.containsKey(position)) {
            return Map.of("code", 1, "msg", "无效的岗位：" + position);
        }

        // 1. 员工必须已注册
        User employee = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        if (employee == null) {
            return Map.of("code", 1, "msg", "该手机号未注册，请先让员工注册");
        }
        // 2. 只能拉员工角色
        if (!"EMPLOYEE".equals(employee.getRole())) {
            return Map.of("code", 1, "msg", "只能拉取员工角色");
        }
        // 3. 员工不能同时属于多个公司
        if (employee.getTenantId() != null && !employee.getTenantId().equals(tenantId)) {
            return Map.of("code", 1, "msg", "该员工已在其他公司");
        }
        // 4. 防止重复拉人
        Long exists = bossEmployeeMapper.selectCount(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employee.getId()));
        if (exists != null && exists > 0) {
            return Map.of("code", 1, "msg", "该员工已在团队中");
        }
        // 5. 指定团队时必须存在且属于本公司；未指定则落入默认团队
        Team team;
        String teamIdStr = body.get("teamId");
        if (teamIdStr != null && !teamIdStr.isBlank()) {
            team = teamMapper.selectById(Long.parseLong(teamIdStr.trim()));
            if (team == null || !tenantId.equals(team.getTenantId())) {
                return Map.of("code", 1, "msg", "指定的团队不存在");
            }
        } else {
            team = ensureDefaultTeam(bossId, tenantId);
        }

        // 6. 建立团队关系
        BossEmployee relation = new BossEmployee();
        relation.setTenantId(tenantId);
        relation.setBossId(bossId);
        relation.setEmployeeId(employee.getId());
        relation.setPosition(position);
        relation.setTeamId(team.getId());
        relation.setStatus(1);
        bossEmployeeMapper.insert(relation);

        // 7. 回填员工租户（首次入队）
        if (employee.getTenantId() == null) {
            employee.setTenantId(tenantId);
            userMapper.updateById(employee);
        }
        return Map.of("code", 0, "msg", "拉人成功");
    }

    // 删除人：移出团队并解除员工的公司归属
    @DeleteMapping("/members/{employeeId}")
    @Transactional
    public Map<String, Object> removeMember(HttpServletRequest request, @PathVariable Long employeeId) {
        Map<String, Object> auth = currentBoss(request);
        if (auth == null) {
            return Map.of("code", 1, "msg", "只有已创建公司的老板才能删除成员");
        }
        Long bossId = (Long) auth.get("bossId");

        BossEmployee relation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employeeId));
        if (relation == null) {
            return Map.of("code", 1, "msg", "该员工不在团队中");
        }
        bossEmployeeMapper.deleteById(relation.getId());

        User employee = userMapper.selectById(employeeId);
        if (employee != null && employee.getTenantId() != null) {
            employee.setTenantId(null);
            userMapper.updateById(employee);
        }
        return Map.of("code", 0, "msg", "删除成功");
    }

    // 分配负责的内容 / 修改分配内容（同一员工一条分配，存在则更新）
    @PutMapping("/members/{employeeId}/assignment")
    public Map<String, Object> assign(HttpServletRequest request, @PathVariable Long employeeId,
                                      @RequestBody Map<String, String> body) {
        Map<String, Object> auth = currentBoss(request);
        if (auth == null) {
            return Map.of("code", 1, "msg", "只有已创建公司的老板才能分配内容");
        }
        Long bossId = (Long) auth.get("bossId");

        String contentType = body.get("contentType");
        String contentIdStr = body.get("contentId");
        if (contentType == null || contentType.isBlank() || contentIdStr == null) {
            return Map.of("code", 1, "msg", "contentType 和 contentId 不能为空");
        }
        long contentId;
        try {
            contentId = Long.parseLong(contentIdStr);
        } catch (NumberFormatException e) {
            return Map.of("code", 1, "msg", "contentId 必须是数字");
        }

        // 员工必须在团队中（boss_employee 带租户条件，天然隔离）
        Long exists = bossEmployeeMapper.selectCount(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employeeId));
        if (exists == null || exists == 0) {
            return Map.of("code", 1, "msg", "该员工不在团队中");
        }

        // upsert：查询自动带 tenant_id 条件，保证只动本租户的分配
        ContentAssignment assignment = contentAssignmentMapper.selectOne(
                Wrappers.<ContentAssignment>lambdaQuery().eq(ContentAssignment::getEmployeeId, employeeId));
        if (assignment == null) {
            assignment = new ContentAssignment();
            assignment.setBossId(bossId);
            assignment.setEmployeeId(employeeId);
            assignment.setContentType(contentType);
            assignment.setContentId(contentId);
            assignment.setStatus(1);
            contentAssignmentMapper.insert(assignment);
        } else {
            assignment.setBossId(bossId);
            assignment.setContentType(contentType);
            assignment.setContentId(contentId);
            contentAssignmentMapper.updateById(assignment);
        }
        return Map.of("code", 0, "msg", "分配成功");
    }

    /**
     * 取公司默认团队，不存在则创建（幂等）
     * 默认团队负责人挂老板本人，保证公司一建立就有一个可用的团队容器
     */
    private Team ensureDefaultTeam(Long bossId, Long tenantId) {
        Team team = teamMapper.selectOne(Wrappers.<Team>lambdaQuery()
                .eq(Team::getTenantId, tenantId)
                .eq(Team::getIsDefault, 1)
                .last("LIMIT 1"));
        if (team != null) {
            return team;
        }
        team = new Team();
        team.setTenantId(tenantId);
        team.setName(DEFAULT_TEAM_NAME);
        team.setLeaderUserId(bossId);
        team.setIsDefault(1);
        team.setStatus(1);
        teamMapper.insert(team);
        return team;
    }

    /**
     * 校验当前请求是否为本租户的老板，返回 {bossId, tenantId}；不是则返回 null
     * 身份来源：网关透传的 X-User-Id 头 + TenantContext 中的租户
     */
    private Map<String, Object> currentBoss(HttpServletRequest request) {
        String uid = request.getHeader("X-User-Id");
        Long tenantId = TenantContext.getTenantId();
        if (uid == null || uid.isBlank() || tenantId == null) {
            return null;
        }
        Long bossId = Long.parseLong(uid.trim());
        User user = userMapper.selectById(bossId);
        if (user == null || !"BOSS".equals(user.getRole()) || !tenantId.equals(user.getTenantId())) {
            return null;
        }
        Map<String, Object> auth = new HashMap<>();
        auth.put("bossId", bossId);
        auth.put("tenantId", tenantId);
        return auth;
    }
}