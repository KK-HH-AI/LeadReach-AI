package com.leadreach.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.Company;
import com.leadreach.user.entity.JoinRequest;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.CompanyMapper;
import com.leadreach.user.mapper.JoinRequestMapper;
import com.leadreach.user.mapper.UserMapper;
import com.leadreach.user.service.JoinRequestService;
import com.leadreach.user.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工申请加入公司业务实现
 * join_request 表不参与租户插件，所有查询条件在本类显式携带
 */
@Service
@RequiredArgsConstructor
public class JoinRequestServiceImpl implements JoinRequestService {

    /** 申请状态 */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";

    /** 审批通过后的默认岗位：销售专员（老板可后续调整） */
    private static final String DEFAULT_POSITION = "SALES_REP";

    private final JoinRequestMapper joinRequestMapper;
    private final CompanyMapper companyMapper;
    private final UserMapper userMapper;
    private final BossEmployeeMapper bossEmployeeMapper;
    private final TeamService teamService;

    // 员工按公司名称搜索公司
    @Override
    public Map<String, Object> searchCompanies(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Map.of("code", 1, "msg", "搜索关键词不能为空");
        }
        List<Company> companies = companyMapper.selectList(Wrappers.<Company>lambdaQuery()
                .like(Company::getName, keyword.trim())
                .orderByAsc(Company::getId)
                .last("LIMIT 20"));
        List<Map<String, Object>> data = new ArrayList<>();
        for (Company c : companies) {
            Map<String, Object> item = new HashMap<>();
            item.put("companyId", c.getId());
            item.put("name", c.getName());
            item.put("industry", c.getIndustry());
            item.put("region", c.getRegion());
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 员工提交加入申请
    @Override
    public Map<String, Object> apply(Long employeeId, Long companyId) {
        if (employeeId == null) {
            return Map.of("code", 1, "msg", "缺少用户身份");
        }
        if (companyId == null) {
            return Map.of("code", 1, "msg", "目标公司不能为空");
        }
        // 1. 必须是未入公司的在职员工
        User employee = userMapper.selectById(employeeId);
        if (employee == null || !"EMPLOYEE".equals(employee.getRole())) {
            return Map.of("code", 1, "msg", "只有员工才能申请加入公司");
        }
        if (employee.getStatus() != null && employee.getStatus() == 0) {
            return Map.of("code", 1, "msg", "账号已禁用");
        }
        if (employee.getTenantId() != null) {
            return Map.of("code", 1, "msg", "你已属于某公司，不能重复申请");
        }
        // 2. 目标公司必须存在
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            return Map.of("code", 1, "msg", "目标公司不存在");
        }
        // 3. 同公司不允许重复待审（被拒后可再次申请）
        Long pending = joinRequestMapper.selectCount(Wrappers.<JoinRequest>lambdaQuery()
                .eq(JoinRequest::getTenantId, companyId)
                .eq(JoinRequest::getEmployeeId, employeeId)
                .eq(JoinRequest::getStatus, STATUS_PENDING));
        if (pending != null && pending > 0) {
            return Map.of("code", 1, "msg", "已提交过申请，请等待老板审批");
        }

        // 4. 写入申请
        JoinRequest request = new JoinRequest();
        request.setTenantId(companyId);
        request.setEmployeeId(employeeId);
        request.setStatus(STATUS_PENDING);
        joinRequestMapper.insert(request);
        return Map.of("code", 0, "msg", "申请已提交，等待老板审批");
    }

    // 员工撤回自己的待审批申请：仅本人 + 仅 PENDING 可撤
    @Override
    public Map<String, Object> withdraw(Long employeeId, Long requestId) {
        if (employeeId == null) {
            return Map.of("code", 1, "msg", "缺少用户身份");
        }
        JoinRequest request = joinRequestMapper.selectById(requestId);
        if (request == null || !employeeId.equals(request.getEmployeeId())) {
            return Map.of("code", 1, "msg", "申请不存在");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            return Map.of("code", 1, "msg", "该申请已处理，无法撤回");
        }
        request.setStatus(STATUS_WITHDRAWN);
        joinRequestMapper.updateById(request);
        return Map.of("code", 0, "msg", "申请已撤回");
    }

    // 员工查看自己的申请记录及状态
    @Override
    public Map<String, Object> myRequests(Long employeeId) {
        if (employeeId == null) {
            return Map.of("code", 1, "msg", "缺少用户身份");
        }
        List<JoinRequest> requests = joinRequestMapper.selectList(Wrappers.<JoinRequest>lambdaQuery()
                .eq(JoinRequest::getEmployeeId, employeeId)
                .orderByDesc(JoinRequest::getId));
        // 公司名映射
        List<Long> companyIds = requests.stream().map(JoinRequest::getTenantId).distinct().toList();
        Map<Long, String> companyNameMap = new HashMap<>();
        if (!companyIds.isEmpty()) {
            companyMapper.selectBatchIds(companyIds).forEach(c -> companyNameMap.put(c.getId(), c.getName()));
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (JoinRequest r : requests) {
            Map<String, Object> item = new HashMap<>();
            item.put("requestId", r.getId());
            item.put("companyId", r.getTenantId());
            item.put("companyName", companyNameMap.get(r.getTenantId()));
            item.put("status", r.getStatus());
            item.put("createdAt", r.getCreatedAt());
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 老板查看本公司的申请列表（含申请人信息与状态）
    @Override
    public Map<String, Object> listRequests(Long bossId, Long tenantId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能查看申请");
        if (err != null) {
            return err;
        }
        List<JoinRequest> requests = joinRequestMapper.selectList(Wrappers.<JoinRequest>lambdaQuery()
                .eq(JoinRequest::getTenantId, tenantId)
                .orderByDesc(JoinRequest::getId));
        // 申请人信息批量取，避免 N+1
        List<Long> employeeIds = requests.stream().map(JoinRequest::getEmployeeId).distinct().toList();
        Map<Long, User> userMap = new HashMap<>();
        if (!employeeIds.isEmpty()) {
            userMapper.selectBatchIds(employeeIds).forEach(u -> userMap.put(u.getId(), u));
        }
        List<Map<String, Object>> data = new ArrayList<>();
        for (JoinRequest r : requests) {
            User u = userMap.get(r.getEmployeeId());
            Map<String, Object> item = new HashMap<>();
            item.put("requestId", r.getId());
            item.put("employeeId", r.getEmployeeId());
            item.put("phone", u == null ? null : u.getPhone());
            item.put("name", u == null ? null : u.getName());
            item.put("email", u == null ? null : u.getEmail());
            item.put("status", r.getStatus());
            item.put("createdAt", r.getCreatedAt());
            data.add(item);
        }
        return Map.of("code", 0, "msg", "ok", "data", data);
    }

    // 老板同意申请：员工入默认团队（岗位默认销售专员）并回填租户
    @Override
    @Transactional
    public Map<String, Object> approve(Long bossId, Long tenantId, Long requestId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能审批申请");
        if (err != null) {
            return err;
        }
        JoinRequest request = joinRequestMapper.selectById(requestId);
        if (request == null || !tenantId.equals(request.getTenantId())) {
            return Map.of("code", 1, "msg", "申请不存在");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            return Map.of("code", 1, "msg", "该申请已处理过");
        }

        // 员工等待期间状态可能变化（加入别家/被禁用），审批前复查
        User employee = userMapper.selectById(request.getEmployeeId());
        if (employee == null || !"EMPLOYEE".equals(employee.getRole())
                || (employee.getStatus() != null && employee.getStatus() == 0)) {
            request.setStatus(STATUS_REJECTED);
            joinRequestMapper.updateById(request);
            return Map.of("code", 1, "msg", "申请人账号异常或已禁用，申请已自动拒绝");
        }
        if (employee.getTenantId() != null && !employee.getTenantId().equals(tenantId)) {
            request.setStatus(STATUS_REJECTED);
            joinRequestMapper.updateById(request);
            return Map.of("code", 1, "msg", "该员工已加入其他公司，申请已自动拒绝");
        }

        // 建立归属关系（仅在职关系算重复：等待期间可能已被拉人接口直接拉入）
        Long exists = bossEmployeeMapper.selectCount(Wrappers.<BossEmployee>lambdaQuery()
                .eq(BossEmployee::getBossId, bossId)
                .eq(BossEmployee::getEmployeeId, employee.getId())
                .eq(BossEmployee::getStatus, 1));
        if (exists == null || exists == 0) {
            Long teamId = teamService.ensureDefaultTeamId(bossId, tenantId);
            // 查离职旧记录：uk_boss_employee 唯一键只有一条，复活而非新建
            BossEmployee oldRelation = bossEmployeeMapper.selectOne(Wrappers.<BossEmployee>lambdaQuery()
                    .eq(BossEmployee::getBossId, bossId)
                    .eq(BossEmployee::getEmployeeId, employee.getId())
                    .last("LIMIT 1"));
            if (oldRelation != null) {
                // 复活用显式 set 更新（岗位分类要清空，updateById 会跳过 null 字段）
                bossEmployeeMapper.update(null, new LambdaUpdateWrapper<BossEmployee>()
                        .eq(BossEmployee::getId, oldRelation.getId())
                        .set(BossEmployee::getPosition, DEFAULT_POSITION)
                        .set(BossEmployee::getTeamId, teamId)
                        .set(BossEmployee::getTeamPositionId, null)
                        .set(BossEmployee::getStatus, 1));
            } else {
                BossEmployee relation = new BossEmployee();
                relation.setTenantId(tenantId);
                relation.setBossId(bossId);
                relation.setEmployeeId(employee.getId());
                relation.setPosition(DEFAULT_POSITION);
                relation.setTeamId(teamId);
                relation.setStatus(1);
                bossEmployeeMapper.insert(relation);
            }
        }
        // 回填员工租户（首次入公司）
        if (employee.getTenantId() == null) {
            employee.setTenantId(tenantId);
            userMapper.updateById(employee);
        }
        // 申请置已同意
        request.setStatus(STATUS_APPROVED);
        joinRequestMapper.updateById(request);
        return Map.of("code", 0, "msg", "已同意，员工正式加入公司");
    }

    // 老板拒绝申请：仅置 REJECTED，员工可另寻公司
    @Override
    public Map<String, Object> reject(Long bossId, Long tenantId, Long requestId) {
        Map<String, Object> err = bossCheckError(bossId, tenantId, "只有已创建公司的老板才能审批申请");
        if (err != null) {
            return err;
        }
        JoinRequest request = joinRequestMapper.selectById(requestId);
        if (request == null || !tenantId.equals(request.getTenantId())) {
            return Map.of("code", 1, "msg", "申请不存在");
        }
        if (!STATUS_PENDING.equals(request.getStatus())) {
            return Map.of("code", 1, "msg", "该申请已处理过");
        }
        request.setStatus(STATUS_REJECTED);
        joinRequestMapper.updateById(request);
        return Map.of("code", 0, "msg", "已拒绝该申请");
    }

    /**
     * 校验当前请求是否为本租户的老板；不合法返回错误响应，合法返回 null
     * 与 TeamServiceImpl 同规则：Controller 解析的 X-User-Id + TenantContext 中的租户
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
