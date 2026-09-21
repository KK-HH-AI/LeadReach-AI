package com.leadreach.user.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leadreach.user.context.TenantContext;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.ContentAssignment;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.ContentAssignmentMapper;
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
 * 团队业务：老板创建团队、拉人、删除人、分配/修改员工负责的内容
 * 团队即 boss_employee 集合，一个老板一个团队，随公司创建而自动可用
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

    @Autowired
    private BossEmployeeMapper bossEmployeeMapper;

    @Autowired
    private ContentAssignmentMapper contentAssignmentMapper;

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
        Map<String, Object> data = new HashMap<>();
        data.put("teamId", tenantId);
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

        List<Map<String, Object>> data = new ArrayList<>();
        for (BossEmployee m : members) {
            Map<String, Object> item = new HashMap<>();
            item.put("employeeId", m.getEmployeeId());
            item.put("phone", phoneMap.getOrDefault(m.getEmployeeId(), ""));
            item.put("position", m.getPosition());
            item.put("createdAt", m.getCreatedAt());
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 拉人入队：员工必须先注册
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

        // 5. 建立团队关系
        BossEmployee relation = new BossEmployee();
        relation.setTenantId(tenantId);
        relation.setBossId(bossId);
        relation.setEmployeeId(employee.getId());
        relation.setPosition(position);
        relation.setStatus(1);
        bossEmployeeMapper.insert(relation);

        // 6. 回填员工租户（首次入队）
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
