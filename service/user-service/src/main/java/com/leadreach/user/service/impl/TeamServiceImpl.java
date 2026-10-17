package com.leadreach.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.ContentAssignment;
import com.leadreach.user.entity.Team;
import com.leadreach.user.entity.TeamPosition;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.ContentAssignmentMapper;
import com.leadreach.user.mapper.TeamMapper;
import com.leadreach.user.mapper.TeamPositionMapper;
import com.leadreach.user.mapper.UserMapper;
import com.leadreach.user.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 团队业务实现：老板管理团队、拉人、删除人、分配/修改员工负责的内容
 *
 * 公司创建后自动拥有一个「默认团队」，未显式指定团队的成员落在默认团队里。
 */
@Service
public class TeamServiceImpl implements TeamService {

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
    private TeamPositionMapper teamPositionMapper;

    @Autowired
    private UserMapper userMapper;

    // 创建团队：幂等确认，返回团队概况
    @Override
    public Map<String, Object> createDefaultTeam(Long bossId, Long tenantId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能创建团队");
        if (err != null) {
            return err;
        }

        long count = bossEmployeeMapper.selectCount(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId));
        Team defaultTeam = ensureDefaultTeam(bossId, tenantId);
        Map<String, Object> data = new HashMap<>();
        data.put("teamId", defaultTeam.getId());
        data.put("bossId", bossId);
        data.put("memberCount", count);
        return Map.of("code", 0, "msg", "团队可用", "data", data);
    }

    // 团队成员列表
    @Override
    public Map<String, Object> listMembers(Long bossId, Long tenantId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能查看团队");
        if (err != null) {
            return err;
        }
        List<BossEmployee> members = bossEmployeeMapper.selectList(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getStatus, 1));

        // 批量取员工手机号（避免 N+1），前端展示需要
        List<Long> empIds = members.stream().map(BossEmployee::getEmployeeId).toList();
        Map<Long, String> phoneMap = new HashMap<>();
        if (!empIds.isEmpty()) {
            userMapper.selectBatchIds(empIds).forEach(u -> phoneMap.put(u.getId(), u.getPhone()));
        }
        // 团队名映射，随成员一起下发，前端可直接展示归属团队
        Map<Long, String> teamNameMap = new HashMap<>();
        teamMapper.selectList(Wrappers.<Team>lambdaQuery().eq(Team::getTenantId, tenantId))
                .forEach(t -> teamNameMap.put(t.getId(), t.getName()));
        // 团队自定义岗位类型名映射
        Map<Long, String> positionNameMap = new HashMap<>();
        teamPositionMapper.selectList(Wrappers.<TeamPosition>lambdaQuery().eq(TeamPosition::getTenantId, tenantId))
                .forEach(p -> positionNameMap.put(p.getId(), p.getName()));

        List<Map<String, Object>> data = new ArrayList<>();
        for (BossEmployee m : members) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", m.getEmployeeId());
            item.put("phone", phoneMap.getOrDefault(m.getEmployeeId(), ""));
            item.put("position", m.getPosition());
            item.put("teamId", m.getTeamId());
            item.put("teamName", m.getTeamId() == null ? null : teamNameMap.get(m.getTeamId()));
            item.put("teamPositionId", m.getTeamPositionId());
            item.put("teamPositionName", m.getTeamPositionId() == null ? null : positionNameMap.get(m.getTeamPositionId()));
            item.put("createdAt", m.getCreatedAt());
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 团队列表：公司下的所有团队，含负责人与人数
    @Override
    public Map<String, Object> listGroups(Long bossId, Long tenantId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能查看团队");
        if (err != null) {
            return err;
        }

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
                .eq(BossEmployee::getTenantId, tenantId)
                .eq(BossEmployee::getStatus, 1));
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
    @Override
    @Transactional
    public Map<String, Object> createGroup(Long bossId, Long tenantId, String name, String leaderPhone) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能创建团队");
        if (err != null) {
            return err;
        }

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

    // 拉人入队：员工必须先注册；teamId 为 null 时落入默认团队
    @Override
    @Transactional
    public Map<String, Object> addMember(Long bossId, Long tenantId, String phone, String position, Long teamId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能拉人");
        if (err != null) {
            return err;
        }

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
        // 4. 查历史关系：在职算重复；离职记录复用（uk_boss_employee 唯一键只有一条，复活而非新建）
        BossEmployee oldRelation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employee.getId())
                .last("LIMIT 1"));
        if (oldRelation != null && oldRelation.getStatus() != null && oldRelation.getStatus() == 1) {
            return Map.of("code", 1, "msg", "该员工已在团队中");
        }
        // 5. 指定团队时必须存在且属于本公司；未指定则落入默认团队
        Team team;
        if (teamId != null) {
            team = teamMapper.selectById(teamId);
            if (team == null || !tenantId.equals(team.getTenantId())) {
                return Map.of("code", 1, "msg", "指定的团队不存在");
            }
        } else {
            team = ensureDefaultTeam(bossId, tenantId);
        }

        // 6. 建立团队关系（有离职记录则复活，否则新建）
        if (oldRelation != null) {
            // 复活用显式 set 更新（岗位分类要清空，updateById 会跳过 null 字段）
            bossEmployeeMapper.update(null, new LambdaUpdateWrapper<BossEmployee>()
                    .eq(BossEmployee::getId, oldRelation.getId())
                    .set(BossEmployee::getPosition, position)
                    .set(BossEmployee::getTeamId, team.getId())
                    .set(BossEmployee::getTeamPositionId, null)
                    .set(BossEmployee::getStatus, 1));
        } else {
            BossEmployee relation = new BossEmployee();
            relation.setTenantId(tenantId);
            relation.setBossId(bossId);
            relation.setEmployeeId(employee.getId());
            relation.setPosition(position);
            relation.setTeamId(team.getId());
            relation.setStatus(1);
            bossEmployeeMapper.insert(relation);
        }

        // 7. 回填员工租户（首次入队）
        if (employee.getTenantId() == null) {
            employee.setTenantId(tenantId);
            userMapper.updateById(employee);
        }
        return Map.of("code", 0, "msg", "拉人成功");
    }

    // 开除员工：软删除团队关系（status 1→0 保留历史，供线索归属溯源），并解除公司归属
    @Override
    @Transactional
    public Map<String, Object> removeMember(Long bossId, Long tenantId, Long employeeId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能删除成员");
        if (err != null) {
            return err;
        }

        BossEmployee relation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employeeId));
        if (relation == null) {
            return Map.of("code", 1, "msg", "该员工不在团队中");
        }
        if (relation.getStatus() != null && relation.getStatus() == 0) {
            return Map.of("code", 1, "msg", "该员工已不在公司中");
        }
        // 软删除：关系记录保留（teamId/position 不动，留作历史），仅标记离职
        relation.setStatus(0);
        bossEmployeeMapper.updateById(relation);

        // 清空员工租户，变回自由账号（可申请/被别家公司拉入）
        User employee = userMapper.selectById(employeeId);
        if (employee != null && employee.getTenantId() != null) {
            employee.setTenantId(null);
            userMapper.updateById(employee);
        }
        return Map.of("code", 0, "msg", "已开除该员工");
    }

    // 踢出团队：不开除，回到公司默认团队；原团队的岗位分类自动清空
    @Override
    public Map<String, Object> removeFromTeam(Long bossId, Long tenantId, Long employeeId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能调整团队");
        if (err != null) {
            return err;
        }
        // 只操作在职员工（开除后的 status=0 记录不允许再调整）
        BossEmployee relation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employeeId)
                .eq(BossEmployee::getStatus, 1));
        if (relation == null) {
            return Map.of("code", 1, "msg", "该员工不在本公司");
        }
        Team defaultTeam = ensureDefaultTeam(bossId, tenantId);
        if (defaultTeam.getId().equals(relation.getTeamId())) {
            return Map.of("code", 1, "msg", "该员工已在默认团队");
        }
        // 岗位分类挂在团队下，踢出后不适用，显式置空（MyBatis-Plus 默认更新跳过 null 字段）
        bossEmployeeMapper.update(null, new LambdaUpdateWrapper<BossEmployee>()
                .eq(BossEmployee::getId, relation.getId())
                .set(BossEmployee::getTeamId, defaultTeam.getId())
                .set(BossEmployee::getTeamPositionId, null));
        return Map.of("code", 0, "msg", "已移出团队，回落默认团队");
    }

    // 分配负责的内容 / 修改分配内容（同一员工一条分配，存在则更新）
    @Override
    public Map<String, Object> assign(Long bossId, Long tenantId, Long employeeId, String contentType, String contentId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能分配内容");
        if (err != null) {
            return err;
        }

        if (contentType == null || contentType.isBlank() || contentId == null) {
            return Map.of("code", 1, "msg", "contentType 和 contentId 不能为空");
        }
        long cid;
        try {
            cid = Long.parseLong(contentId);
        } catch (NumberFormatException e) {
            return Map.of("code", 1, "msg", "contentId 必须是数字");
        }

        // 员工必须在团队中（boss_employee 带租户条件，天然隔离；离职记录不可分配）
        Long exists = bossEmployeeMapper.selectCount(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employeeId)
                .eq(BossEmployee::getStatus, 1));
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
            assignment.setContentId(cid);
            assignment.setStatus(1);
            contentAssignmentMapper.insert(assignment);
        } else {
            assignment.setBossId(bossId);
            assignment.setContentType(contentType);
            assignment.setContentId(cid);
            contentAssignmentMapper.updateById(assignment);
        }
        return Map.of("code", 0, "msg", "分配成功");
    }

    // 拉人检索：按 姓名/邮箱/手机 模糊匹配员工（自由账号 + 本公司员工，SQL 内限定防跨租户泄露）
    @Override
    public Map<String, Object> searchMembers(Long bossId, Long tenantId, String keyword) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能拉人");
        if (err != null) {
            return err;
        }
        if (keyword == null || keyword.isBlank()) {
            return Map.of("code", 1, "msg", "搜索关键词不能为空");
        }
        List<User> users = userMapper.searchEmployeesByKeyword(keyword.trim(), tenantId);
        List<Map<String, Object>> data = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getId());
            item.put("phone", u.getPhone());
            item.put("name", u.getName());
            item.put("email", u.getEmail());
            // tenantId 为 null = 自由账号（未加入任何公司），可直接拉入
            item.put("tenantId", u.getTenantId());
            item.put("joined", u.getTenantId() != null);
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 为团队创建自定义岗位类型
    @Override
    public Map<String, Object> createTeamPosition(Long bossId, Long tenantId, Long teamId, String name) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能设置岗位类型");
        if (err != null) {
            return err;
        }
        Team team = requireTeamOfTenant(teamId, tenantId);
        if (team == null) {
            return Map.of("code", 1, "msg", "团队不存在");
        }
        if (name == null || name.isBlank()) {
            return Map.of("code", 1, "msg", "岗位类型名称不能为空");
        }
        String posName = name.trim();
        Long exists = teamPositionMapper.selectCount(Wrappers.<TeamPosition>lambdaQuery()
                .eq(TeamPosition::getTeamId, teamId)
                .eq(TeamPosition::getName, posName));
        if (exists != null && exists > 0) {
            return Map.of("code", 1, "msg", "该团队下同名岗位类型已存在");
        }

        TeamPosition position = new TeamPosition();
        position.setTenantId(tenantId);
        position.setTeamId(teamId);
        position.setName(posName);
        teamPositionMapper.insert(position);

        Map<String, Object> data = new HashMap<>();
        data.put("teamPositionId", position.getId());
        data.put("teamId", teamId);
        data.put("name", posName);
        return Map.of("code", 0, "msg", "创建岗位类型成功", "data", data);
    }

    // 团队岗位类型列表
    @Override
    public Map<String, Object> listTeamPositions(Long bossId, Long tenantId, Long teamId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能查看岗位类型");
        if (err != null) {
            return err;
        }
        Team team = requireTeamOfTenant(teamId, tenantId);
        if (team == null) {
            return Map.of("code", 1, "msg", "团队不存在");
        }
        List<TeamPosition> positions = teamPositionMapper.selectList(Wrappers.<TeamPosition>lambdaQuery()
                .eq(TeamPosition::getTeamId, teamId)
                .orderByAsc(TeamPosition::getId));
        List<Map<String, Object>> data = new ArrayList<>();
        for (TeamPosition p : positions) {
            Map<String, Object> item = new HashMap<>();
            item.put("teamPositionId", p.getId());
            item.put("teamId", p.getTeamId());
            item.put("name", p.getName());
            item.put("createdAt", p.getCreatedAt());
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 删除团队岗位类型：物理删除（配置数据，避免唯一键与软删记录冲突），引用它的成员分类自动置空
    @Override
    @Transactional
    public Map<String, Object> deleteTeamPosition(Long bossId, Long tenantId, Long teamId, Long positionId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能删除岗位类型");
        if (err != null) {
            return err;
        }
        TeamPosition position = teamPositionMapper.selectById(positionId);
        if (position == null || !tenantId.equals(position.getTenantId()) || !teamId.equals(position.getTeamId())) {
            return Map.of("code", 1, "msg", "岗位类型不存在");
        }
        // 清空成员引用（显式 set(null)：MyBatis-Plus 默认更新会跳过 null 字段）
        bossEmployeeMapper.update(null, new LambdaUpdateWrapper<BossEmployee>()
                .eq(BossEmployee::getTeamPositionId, positionId)
                .set(BossEmployee::getTeamPositionId, null));
        teamPositionMapper.deleteById(positionId);
        return Map.of("code", 0, "msg", "删除岗位类型成功");
    }

    // 把成员分类到团队岗位类型（成员必须在该岗位所属团队中）
    @Override
    public Map<String, Object> assignTeamPosition(Long bossId, Long tenantId, Long employeeId, Long teamPositionId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能分配岗位");
        if (err != null) {
            return err;
        }
        if (teamPositionId == null) {
            return Map.of("code", 1, "msg", "teamPositionId 不能为空");
        }
        TeamPosition position = teamPositionMapper.selectById(teamPositionId);
        if (position == null || !tenantId.equals(position.getTenantId())) {
            return Map.of("code", 1, "msg", "岗位类型不存在");
        }
        // 员工必须在本老板团队中（查询带租户条件，天然隔离；离职记录不可操作）
        BossEmployee relation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employeeId)
                .eq(BossEmployee::getStatus, 1));
        if (relation == null) {
            return Map.of("code", 1, "msg", "该员工不在团队中");
        }
        if (!position.getTeamId().equals(relation.getTeamId())) {
            return Map.of("code", 1, "msg", "员工不在该岗位类型所属的团队中");
        }
        relation.setTeamPositionId(teamPositionId);
        bossEmployeeMapper.updateById(relation);
        return Map.of("code", 0, "msg", "分配岗位成功");
    }

    // 把已在本公司的员工分配/调整到指定团队
    @Override
    public Map<String, Object> assignTeam(Long bossId, Long tenantId, Long employeeId, Long teamId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能分配团队");
        if (err != null) {
            return err;
        }
        if (teamId == null) {
            return Map.of("code", 1, "msg", "teamId 不能为空");
        }
        // 员工必须已在本公司（先入公司，才能进团队；开除后的离职记录不可操作）
        BossEmployee relation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employeeId)
                .eq(BossEmployee::getStatus, 1));
        if (relation == null) {
            return Map.of("code", 1, "msg", "该员工不在本公司，请先拉人或审批加入申请");
        }
        // 目标团队必须属于本公司
        Team team = requireTeamOfTenant(teamId, tenantId);
        if (team == null) {
            return Map.of("code", 1, "msg", "目标团队不存在");
        }
        // 岗位分类挂在团队下，换团队后原分类不适用，显式置空（MyBatis-Plus 默认更新跳过 null 字段）
        bossEmployeeMapper.update(null, new LambdaUpdateWrapper<BossEmployee>()
                .eq(BossEmployee::getId, relation.getId())
                .set(BossEmployee::getTeamId, teamId)
                .set(BossEmployee::getTeamPositionId, null));
        return Map.of("code", 0, "msg", "分配团队成功");
    }

    /** 校验团队存在且属于本租户；不满足返回 null 由调用方兜底 */
    private Team requireTeamOfTenant(Long teamId, Long tenantId) {
        if (teamId == null) {
            return null;
        }
        Team team = teamMapper.selectById(teamId);
        if (team == null || !tenantId.equals(team.getTenantId())) {
            return null;
        }
        return team;
    }

    /** 取公司默认团队ID，不存在则创建（幂等）；供员工加入公司等流程复用 */
    @Override
    public Long ensureDefaultTeamId(Long bossId, Long tenantId) {
        return ensureDefaultTeam(bossId, tenantId).getId();
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
     * 校验当前请求是否为本租户的老板；不合法返回错误响应，合法返回 null
     * 身份来源：Controller 解析的 X-User-Id 与 TenantContext 中的租户
     */
    private Map<String, Object> bossCheckError(Long bossId, Long tenantId, String failMsg) {
        if (bossId == null || tenantId == null) {
            return Map.of("code", 1, "msg", failMsg);
        }
        User user = userMapper.selectById(bossId);
        if (user == null || !"BOSS".equals(user.getRole()) || !tenantId.equals(user.getTenantId())) {
            return Map.of("code", 1, "msg", failMsg);
        }
        return null;
    }
}
