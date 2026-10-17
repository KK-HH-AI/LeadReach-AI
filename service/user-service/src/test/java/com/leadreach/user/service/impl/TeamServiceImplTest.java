package com.leadreach.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.Team;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.ContentAssignmentMapper;
import com.leadreach.user.mapper.TeamMapper;
import com.leadreach.user.mapper.TeamPositionMapper;
import com.leadreach.user.mapper.UserMapper;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 团队管理单元测试：拉人校验链、离职复活、开除软删、踢出团队、调团队
 */
@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    /**
     * 纯 Mockito 单测没有 MyBatis-Plus 运行环境，
     * LambdaUpdateWrapper.set(...) 会立即解析实体列名，需手动初始化实体元数据缓存
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, BossEmployee.class);
        TableInfoHelper.initTableInfo(assistant, Team.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @Mock
    private BossEmployeeMapper bossEmployeeMapper;

    @Mock
    private ContentAssignmentMapper contentAssignmentMapper;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private TeamPositionMapper teamPositionMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private TeamServiceImpl teamService;

    private void stubBoss() {
        User boss = new User();
        boss.setId(1L);
        boss.setRole("BOSS");
        boss.setTenantId(100L);
        lenient().when(userMapper.selectById(1L)).thenReturn(boss);
    }

    private User employee(Long tenantId, String role) {
        User u = new User();
        u.setId(2L);
        u.setPhone("13800000000");
        u.setRole(role);
        u.setTenantId(tenantId);
        return u;
    }

    private Team defaultTeam() {
        Team t = new Team();
        t.setId(10L);
        t.setTenantId(100L);
        t.setIsDefault(1);
        return t;
    }

    // ---------- addMember 拉人 ----------

    @Test
    void addMember_phoneNotRegistered_rejected() {
        stubBoss();
        when(userMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> resp = teamService.addMember(1L, 100L, "13800000000", "SALES_REP", null);
        assertEquals(1, resp.get("code"));
        assertEquals("该手机号未注册，请先让员工注册", resp.get("msg"));
    }

    @Test
    void addMember_notEmployeeRole_rejected() {
        stubBoss();
        when(userMapper.selectOne(any())).thenReturn(employee(null, "BOSS"));

        Map<String, Object> resp = teamService.addMember(1L, 100L, "13800000000", "SALES_REP", null);
        assertEquals(1, resp.get("code"));
        assertEquals("只能拉取员工角色", resp.get("msg"));
    }

    @Test
    void addMember_inOtherCompany_rejected() {
        stubBoss();
        when(userMapper.selectOne(any())).thenReturn(employee(999L, "EMPLOYEE"));

        Map<String, Object> resp = teamService.addMember(1L, 100L, "13800000000", "SALES_REP", null);
        assertEquals(1, resp.get("code"));
        assertEquals("该员工已在其他公司", resp.get("msg"));
    }

    @Test
    void addMember_alreadyActive_rejected() {
        stubBoss();
        when(userMapper.selectOne(any())).thenReturn(employee(100L, "EMPLOYEE"));
        BossEmployee active = new BossEmployee();
        active.setStatus(1);
        when(bossEmployeeMapper.selectOne(any())).thenReturn(active);

        Map<String, Object> resp = teamService.addMember(1L, 100L, "13800000000", "SALES_REP", null);
        assertEquals(1, resp.get("code"));
        assertEquals("该员工已在团队中", resp.get("msg"));
    }

    @Test
    void addMember_removedRecordRevived_notInserted() {
        stubBoss();
        when(userMapper.selectOne(any())).thenReturn(employee(null, "EMPLOYEE"));
        // 历史离职记录（被开除过）
        BossEmployee removed = new BossEmployee();
        removed.setId(77L);
        removed.setStatus(0);
        when(bossEmployeeMapper.selectOne(any())).thenReturn(removed);
        when(teamMapper.selectOne(any())).thenReturn(defaultTeam());

        Map<String, Object> resp = teamService.addMember(1L, 100L, "13800000000", "SALES_REP", null);
        assertEquals(0, resp.get("code"));
        // 复活走 update，不新建（uk_boss_employee 唯一键约束）
        verify(bossEmployeeMapper).update(any(), any());
        verify(bossEmployeeMapper, never()).insert(any(BossEmployee.class));
        // 回填租户
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(100L, captor.getValue().getTenantId());
    }

    @Test
    void addMember_newMember_insertedWithBackfill() {
        stubBoss();
        when(userMapper.selectOne(any())).thenReturn(employee(null, "EMPLOYEE"));
        when(bossEmployeeMapper.selectOne(any())).thenReturn(null);
        when(teamMapper.selectOne(any())).thenReturn(defaultTeam());

        Map<String, Object> resp = teamService.addMember(1L, 100L, "13800000000", "SALES_REP", null);
        assertEquals(0, resp.get("code"));
        verify(bossEmployeeMapper).insert(any(BossEmployee.class));
        verify(userMapper).updateById(any(User.class));
    }

    // ---------- removeMember 开除 ----------

    @Test
    void removeMember_softDeletesAndClearsTenant() {
        stubBoss();
        BossEmployee relation = new BossEmployee();
        relation.setId(77L);
        relation.setEmployeeId(2L);
        relation.setStatus(1);
        when(bossEmployeeMapper.selectOne(any())).thenReturn(relation);
        User emp = employee(100L, "EMPLOYEE");
        when(userMapper.selectById(2L)).thenReturn(emp);

        Map<String, Object> resp = teamService.removeMember(1L, 100L, 2L);
        assertEquals(0, resp.get("code"));
        // 软删除：status 1→0，不物理删除
        ArgumentCaptor<BossEmployee> captor = ArgumentCaptor.forClass(BossEmployee.class);
        verify(bossEmployeeMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        verify(bossEmployeeMapper, never()).deleteById(any(Long.class));
        // 员工变回自由账号
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(null, userCaptor.getValue().getTenantId());
    }

    @Test
    void removeMember_alreadyRemoved_idempotent() {
        stubBoss();
        BossEmployee relation = new BossEmployee();
        relation.setId(77L);
        relation.setStatus(0);
        when(bossEmployeeMapper.selectOne(any())).thenReturn(relation);

        Map<String, Object> resp = teamService.removeMember(1L, 100L, 2L);
        assertEquals(1, resp.get("code"));
        assertEquals("该员工已不在公司中", resp.get("msg"));
    }

    // ---------- removeFromTeam 踢出团队 ----------

    @Test
    void removeFromTeam_movesBackToDefaultTeam() {
        stubBoss();
        BossEmployee relation = new BossEmployee();
        relation.setId(77L);
        relation.setEmployeeId(2L);
        relation.setTeamId(20L);
        relation.setStatus(1);
        when(bossEmployeeMapper.selectOne(any())).thenReturn(relation);
        when(teamMapper.selectOne(any())).thenReturn(defaultTeam());

        Map<String, Object> resp = teamService.removeFromTeam(1L, 100L, 2L);
        assertEquals(0, resp.get("code"));
        verify(bossEmployeeMapper).update(any(), any());
    }

    @Test
    void removeFromTeam_alreadyInDefault_rejected() {
        stubBoss();
        BossEmployee relation = new BossEmployee();
        relation.setId(77L);
        relation.setEmployeeId(2L);
        relation.setTeamId(10L); // 默认团队
        relation.setStatus(1);
        when(bossEmployeeMapper.selectOne(any())).thenReturn(relation);
        when(teamMapper.selectOne(any())).thenReturn(defaultTeam());

        Map<String, Object> resp = teamService.removeFromTeam(1L, 100L, 2L);
        assertEquals(1, resp.get("code"));
        assertEquals("该员工已在默认团队", resp.get("msg"));
    }

    // ---------- assignTeam 调团队 ----------

    @Test
    void assignTeam_removedEmployee_rejected() {
        stubBoss();
        // 开除后的死记录：selectOne 带 status=1 过滤后返回 null
        when(bossEmployeeMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> resp = teamService.assignTeam(1L, 100L, 2L, 20L);
        assertEquals(1, resp.get("code"));
        assertEquals("该员工不在本公司，请先拉人或审批加入申请", resp.get("msg"));
    }
}
