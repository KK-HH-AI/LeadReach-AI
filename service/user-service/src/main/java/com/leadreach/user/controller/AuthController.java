package com.leadreach.user.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.jwt.JWTUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证业务：短信验证码、注册（老板/员工）、验证码登录
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserMapper userMapper;

    /** 短信验证码 Redis key 前缀（发送/校验/删除必须用同一个 key，否则验证码永远删不掉） */
    private static final String SMS_CODE_PREFIX = "sms:code";

    /** 身份角色白名单：注册时只能二选一 */
    private static final Map<String, String> ROLES = Map.of("BOSS", "老板", "EMPLOYEE", "员工");

    /** JWT 签名密钥：必须与网关 gateway-service 配置的 jwt.secret 一致 */
    private static final String JWT_SECRET = "your-secret-key";

    // 发送短信验证码（模拟）
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = RandomUtil.randomNumbers(6);
        // 5 分钟后过期
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, 5, TimeUnit.MINUTES);
        // 测试用，控制台打印短信
        System.out.println("【模拟短信】手机号：" + phone + ", 验证码：" + code);
        // 模拟阶段：把验证码直接返回给前端展示，方便联调；生产环境应接真实短信通道，不能返回验证码
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        data.put("smsCode", code);
        return Map.of("code", 0, "msg", "验证码已发送", "data", data);
    }

    // 注册：手机号 + 验证码 + 密码 + 身份角色；注册时未加入任何公司（tenantId 为空）
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        String password = body.get("password");
        String role = body.get("role");

        // 1. 校验验证码
        if (!verifyCode(phone, code)) {
            return Map.of("code", 1, "msg", "验证码错误或已经过期");
        }
        // 2. 校验角色
        if (!ROLES.containsKey(role)) {
            return Map.of("code", 1, "msg", "无效的身份角色：" + role);
        }
        // 3. 校验密码（6-20 位）
        if (password == null || password.length() < 6 || password.length() > 20) {
            return Map.of("code", 1, "msg", "密码长度须为 6-20 位");
        }
        // 4. 手机号唯一，不能重复注册
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        if (exists != null && exists > 0) {
            return Map.of("code", 1, "msg", "该手机号已注册");
        }

        // 5. 创建账号（密码存 BCrypt 哈希，不存明文）
        User user = new User();
        user.setPhone(phone);
        user.setPassword(BCrypt.hashpw(password));
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);

        consumeCode(phone);
        return Map.of("code", 0, "msg", "注册成功");
    }

    // 登录：手机号 + 密码，校验通过后签发 JWT
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String password = body.get("password");

        // 1. 必须是已注册用户
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getPhone, phone));
        if (user == null) {
            return Map.of("code", 1, "msg", "该手机号未注册，请先注册");
        }
        // 2. 校验密码
        if (user.getPassword() == null || user.getPassword().isEmpty()
                || !BCrypt.checkpw(password, user.getPassword())) {
            return Map.of("code", 1, "msg", "密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Map.of("code", 1, "msg", "账号已禁用");
        }

        // 3. 签发 JWT：claims 携带 uid/role/tenantId，网关校验后透传 X-User-* 头给下游
        //    uid/tenantId 必须转成字符串：网关(golang)断言 claims["uid"] 为 string，数字会失败
        Map<String, Object> payload = new HashMap<>();
        payload.put("uid", String.valueOf(user.getId()));
        payload.put("phone", phone);
        payload.put("role", user.getRole());
        if (user.getTenantId() != null) {
            payload.put("tenantId", String.valueOf(user.getTenantId()));
        }
        payload.put("exp", System.currentTimeMillis() + 24 * 3600 * 1000);
        String token = JWTUtil.createToken(payload, JWT_SECRET.getBytes());

        // 验证码一次性
        consumeCode(phone);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("phone", phone);
        data.put("role", user.getRole());
        data.put("tenantId", user.getTenantId());
        return Map.of("code", 0, "msg", "登录成功", "data", data);
    }

    // 校验验证码是否正确
    private boolean verifyCode(String phone, String code) {
        String cached = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
        return cached != null && cached.equals(code);
    }

    // 验证码一次性，用后删除
    private void consumeCode(String phone) {
        redisTemplate.delete(SMS_CODE_PREFIX + phone);
    }
}
