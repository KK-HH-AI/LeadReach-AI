package com.leadreach.user.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.leadreach.user.entity.BossEmployee;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.BossEmployeeMapper;
import com.leadreach.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证单元测试：验证码、注册校验链、登录签发 JWT
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String KEY = "sms:code13800000000";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserMapper userMapper;

    @Mock
    private BossEmployeeMapper bossEmployeeMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private void stubRedisGet(String value) {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(KEY)).thenReturn(value);
    }

    // ---------- sendCode ----------

    @Test
    void sendCode_storesInRedisWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> resp = authService.sendCode("13800000000");
        assertEquals(0, resp.get("code"));
        // 验证码写入 Redis 且 5 分钟过期
        verify(valueOperations).set(eq(KEY), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    // ---------- register ----------

    @Test
    void register_wrongCode_rejected() {
        stubRedisGet("111111");

        Map<String, Object> resp = authService.register("13800000000", "222222", "123456", "BOSS", null, null);
        assertEquals(1, resp.get("code"));
        assertEquals("验证码错误或已经过期", resp.get("msg"));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_invalidRole_rejected() {
        stubRedisGet("111111");

        Map<String, Object> resp = authService.register("13800000000", "111111", "123456", "ADMIN", null, null);
        assertEquals(1, resp.get("code"));
        assertEquals("无效的身份角色：ADMIN", resp.get("msg"));
    }

    @Test
    void register_shortPassword_rejected() {
        stubRedisGet("111111");

        Map<String, Object> resp = authService.register("13800000000", "111111", "123", "BOSS", null, null);
        assertEquals(1, resp.get("code"));
        assertEquals("密码长度须为 6-20 位", resp.get("msg"));
    }

    @Test
    void register_duplicatePhone_rejected() {
        stubRedisGet("111111");
        when(userMapper.selectCount(any())).thenReturn(1L);

        Map<String, Object> resp = authService.register("13800000000", "111111", "123456", "BOSS", null, null);
        assertEquals(1, resp.get("code"));
        assertEquals("该手机号已注册", resp.get("msg"));
    }

    @Test
    void register_success_storesBcryptHashAndConsumesCode() {
        stubRedisGet("111111");
        when(userMapper.selectCount(any())).thenReturn(0L);

        Map<String, Object> resp = authService.register("13800000000", "111111", "123456", "EMPLOYEE", "张三", "z@x.com");
        assertEquals(0, resp.get("code"));
        // 密码以 BCrypt 哈希落库，不存明文
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertNotEquals("123456", captor.getValue().getPassword());
        assertTrue(BCrypt.checkpw("123456", captor.getValue().getPassword()));
        assertEquals("张三", captor.getValue().getName());
        // 验证码一次性
        verify(redisTemplate).delete(KEY);
    }

    // ---------- login ----------

    @Test
    void login_unregistered_rejected() {
        when(userMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> resp = authService.login("13800000000", "123456");
        assertEquals(1, resp.get("code"));
        assertEquals("该手机号未注册，请先注册", resp.get("msg"));
    }

    @Test
    void login_wrongPassword_rejected() {
        User user = newUser(1L, "BOSS", 1, null);
        user.setPassword(BCrypt.hashpw("123456"));
        when(userMapper.selectOne(any())).thenReturn(user);

        Map<String, Object> resp = authService.login("13800000000", "000000");
        assertEquals(1, resp.get("code"));
        assertEquals("密码错误", resp.get("msg"));
    }

    @Test
    void login_disabled_rejected() {
        User user = newUser(1L, "BOSS", 0, null);
        user.setPassword(BCrypt.hashpw("123456"));
        when(userMapper.selectOne(any())).thenReturn(user);

        Map<String, Object> resp = authService.login("13800000000", "123456");
        assertEquals(1, resp.get("code"));
        assertEquals("账号已禁用", resp.get("msg"));
    }

    @Test
    void login_bossSuccess_issuesTokenWithBossPosition() {
        User user = newUser(1L, "BOSS", 1, 100L);
        user.setPassword(BCrypt.hashpw("123456"));
        when(userMapper.selectOne(any())).thenReturn(user);

        Map<String, Object> resp = authService.login("13800000000", "123456");
        assertEquals(0, resp.get("code"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertTrue(data.get("token") != null && !((String) data.get("token")).isEmpty());
        assertEquals("BOSS", data.get("position"));
        assertEquals(100L, data.get("tenantId"));
        verify(bossEmployeeMapper, never()).selectActiveByEmployeeIdWithoutTenant(any());
    }

    @Test
    void login_employeeSuccess_positionAndTeamFromRelation() {
        User user = newUser(2L, "EMPLOYEE", 1, 100L);
        user.setPassword(BCrypt.hashpw("123456"));
        when(userMapper.selectOne(any())).thenReturn(user);

        BossEmployee relation = new BossEmployee();
        relation.setPosition("SALES_REP");
        relation.setTeamId(5L);
        when(bossEmployeeMapper.selectActiveByEmployeeIdWithoutTenant(2L)).thenReturn(relation);

        Map<String, Object> resp = authService.login("13800000000", "123456");
        assertEquals(0, resp.get("code"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertEquals("SALES_REP", data.get("position"));
        assertEquals(5L, data.get("teamId"));
    }

    private User newUser(Long id, String role, Integer status, Long tenantId) {
        User u = new User();
        u.setId(id);
        u.setPhone("13800000000");
        u.setRole(role);
        u.setStatus(status);
        u.setTenantId(tenantId);
        return u;
    }
}
