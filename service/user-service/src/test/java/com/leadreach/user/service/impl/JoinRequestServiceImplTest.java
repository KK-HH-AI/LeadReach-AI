package com.leadreach.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.Company;
import com.leadreach.user.entity.JoinRequest;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.CompanyMapper;
import com.leadreach.user.mapper.JoinRequestMapper;
import com.leadreach.user.mapper.UserMapper;
import com.leadreach.user.service.TeamService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 员工申请加入公司单元测试：申请校验链、审批状态机、撤回
 */
@ExtendWith(MockitoExtension.class)
class JoinRequestServiceImplTest {

    /**
     * 纯 Mockito 单测没有 MyBatis-Plus 运行环境，
     * LambdaUpdateWrapper.set(...) 会立即解析实体列名，需手动初始化实体元数据缓存
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BossEmployee.class);
        TableInfoHelper.initTableInfo(assistant, JoinRequest.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @Mock
    private JoinRequestMapper joinRequestMapper;

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BossEmployeeMapper bossEmployeeMapper;

    @Mock
    private TeamService teamService;

    @InjectMocks
    private JoinRequestServiceImpl joinRequestService;

    private User employee(Long tenantId, Integer status) {
        User u = new User();
        u.setId(2L);
        u.setRole("EMPLOYEE");
        u.setTenantId(tenantId);
        u.setStatus(status);
        return u;
    }

    private User boss() {
        User u = new User();
        u.setId(1L);
        u.setRole("BOSS");
        u.setTenantId(100L);
        return u;
    }

    // ---------- apply 提交申请 ----------

    @Test
    void apply_alreadyInCompany_rejected() {
        when(userMapper.selectById(2L)).thenReturn(employee(100L, 1));

        Map<String, Object> resp = joinRequestService.apply(2L, 100L);
        assertEquals(1, resp.get("code"));
        assertEquals("你已属于某公司，不能重复申请", resp.get("msg"));
    }

    @Test
    void apply_companyNotFound_rejected() {
        when(userMapper.selectById(2L)).thenReturn(employee(null, 1));
        when(companyMapper.selectById(100L)).thenReturn(null);

        Map<String, Object> resp = joinRequestService.apply(2L, 100L);
        assertEquals(1, resp.get("code"));
        assertEquals("目标公司不存在", resp.get("msg"));
    }

    @Test
    void apply_duplicatePending_rejected() {
        when(userMapper.selectById(2L)).thenReturn(employee(null, 1));
        Company company = new Company();
        company.setId(100L);
        when(companyMapper.selectById(100L)).thenReturn(company);
        when(joinRequestMapper.selectCount(any())).thenReturn(1L);

        Map<String, Object> resp = joinRequestService.apply(2L, 100L);
        assertEquals(1, resp.get("code"));
        assertEquals("已提交过申请，请等待老板审批", resp.get("msg"));
    }

    @Test
    void apply_success_createsPending() {
        when(userMapper.selectById(2L)).thenReturn(employee(null, 1));
        Company company = new Company();
        company.setId(100L);
        when(companyMapper.selectById(100L)).thenReturn(company);
        when(joinRequestMapper.selectCount(any())).thenReturn(0L);

        Map<String, Object> resp = joinRequestService.apply(2L, 100L);
        assertEquals(0, resp.get("code"));
        ArgumentCaptor<JoinRequest> captor = ArgumentCaptor.forClass(JoinRequest.class);
        verify(joinRequestMapper).insert(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals(100L, captor.getValue().getTenantId());
        assertEquals(2L, captor.getValue().getEmployeeId());
    }

    // ---------- approve 审批 ----------

    @Test
    void approve_notBoss_rejected() {
        when(userMapper.selectById(1L)).thenReturn(employee(null, 1)); // 身份不是老板

        Map<String, Object> resp = joinRequestService.approve(1L, 100L, 9L);
        assertEquals(1, resp.get("code"));
        assertEquals("只有已创建公司的老板才能审批申请", resp.get("msg"));
    }

    @Test
    void approve_alreadyProcessed_rejected() {
        when(userMapper.selectById(1L)).thenReturn(boss());
        JoinRequest request = new JoinRequest();
        request.setId(9L);
        request.setTenantId(100L);
        request.setStatus("APPROVED");
        when(joinRequestMapper.selectById(9L)).thenReturn(request);

        Map<String, Object> resp = joinRequestService.approve(1L, 100L, 9L);
        assertEquals(1, resp.get("code"));
        assertEquals("该申请已处理过", resp.get("msg"));
    }

    @Test
    void approve_employeeJoinedOtherCompany_autoRejects() {
        when(userMapper.selectById(1L)).thenReturn(boss());
        JoinRequest request = new JoinRequest();
        request.setId(9L);
        request.setTenantId(100L);
        request.setEmployeeId(2L);
        request.setStatus("PENDING");
        when(joinRequestMapper.selectById(9L)).thenReturn(request);
        when(userMapper.selectById(2L)).thenReturn(employee(999L, 1)); // 等待期间入了别家

        Map<String, Object> resp = joinRequestService.approve(1L, 100L, 9L);
        assertEquals(1, resp.get("code"));
        assertEquals("该员工已加入其他公司，申请已自动拒绝", resp.get("msg"));
        verify(joinRequestMapper).updateById(any(JoinRequest.class));
        verify(bossEmployeeMapper, never()).insert(any(BossEmployee.class));
    }

    @Test
    void approve_success_newRelationBackfillsTenant() {
        when(userMapper.selectById(1L)).thenReturn(boss());
        JoinRequest request = new JoinRequest();
        request.setId(9L);
        request.setTenantId(100L);
        request.setEmployeeId(2L);
        request.setStatus("PENDING");
        when(joinRequestMapper.selectById(9L)).thenReturn(request);
        when(userMapper.selectById(2L)).thenReturn(employee(null, 1));
        when(bossEmployeeMapper.selectCount(any())).thenReturn(0L);
        when(teamService.ensureDefaultTeamId(1L, 100L)).thenReturn(10L);
        when(bossEmployeeMapper.selectOne(any())).thenReturn(null); // 无历史记录

        Map<String, Object> resp = joinRequestService.approve(1L, 100L, 9L);
        assertEquals(0, resp.get("code"));
        verify(bossEmployeeMapper).insert(any(BossEmployee.class));
        // 租户回填
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(100L, userCaptor.getValue().getTenantId());
        // 申请置已同意
        ArgumentCaptor<JoinRequest> reqCaptor = ArgumentCaptor.forClass(JoinRequest.class);
        verify(joinRequestMapper).updateById(reqCaptor.capture());
        assertEquals("APPROVED", reqCaptor.getValue().getStatus());
    }

    @Test
    void approve_success_revivesRemovedRelation() {
        when(userMapper.selectById(1L)).thenReturn(boss());
        JoinRequest request = new JoinRequest();
        request.setId(9L);
        request.setTenantId(100L);
        request.setEmployeeId(2L);
        request.setStatus("PENDING");
        when(joinRequestMapper.selectById(9L)).thenReturn(request);
        when(userMapper.selectById(2L)).thenReturn(employee(null, 1)); // 曾被开除，tenantId 已清
        when(bossEmployeeMapper.selectCount(any())).thenReturn(0L);
        when(teamService.ensureDefaultTeamId(1L, 100L)).thenReturn(10L);
        // 历史离职记录存在
        BossEmployee removed = new BossEmployee();
        removed.setId(77L);
        removed.setStatus(0);
        when(bossEmployeeMapper.selectOne(any())).thenReturn(removed);

        Map<String, Object> resp = joinRequestService.approve(1L, 100L, 9L);
        assertEquals(0, resp.get("code"));
        // 复活而非新建（uk_boss_employee 唯一键）
        verify(bossEmployeeMapper).update(any(), any());
        verify(bossEmployeeMapper, never()).insert(any(BossEmployee.class));
    }

    // ---------- withdraw 撤回 ----------

    @Test
    void withdraw_notOwner_rejected() {
        JoinRequest request = new JoinRequest();
        request.setId(9L);
        request.setEmployeeId(3L); // 别人的申请
        when(joinRequestMapper.selectById(9L)).thenReturn(request);

        Map<String, Object> resp = joinRequestService.withdraw(2L, 9L);
        assertEquals(1, resp.get("code"));
        assertEquals("申请不存在", resp.get("msg"));
    }

    @Test
    void withdraw_processedRequest_rejected() {
        JoinRequest request = new JoinRequest();
        request.setId(9L);
        request.setEmployeeId(2L);
        request.setStatus("APPROVED");
        when(joinRequestMapper.selectById(9L)).thenReturn(request);

        Map<String, Object> resp = joinRequestService.withdraw(2L, 9L);
        assertEquals(1, resp.get("code"));
        assertEquals("该申请已处理，无法撤回", resp.get("msg"));
    }

    @Test
    void withdraw_pending_success() {
        JoinRequest request = new JoinRequest();
        request.setId(9L);
        request.setEmployeeId(2L);
        request.setStatus("PENDING");
        when(joinRequestMapper.selectById(9L)).thenReturn(request);

        Map<String, Object> resp = joinRequestService.withdraw(2L, 9L);
        assertEquals(0, resp.get("code"));
        ArgumentCaptor<JoinRequest> captor = ArgumentCaptor.forClass(JoinRequest.class);
        verify(joinRequestMapper).updateById(captor.capture());
        assertEquals("WITHDRAWN", captor.getValue().getStatus());
    }
}
