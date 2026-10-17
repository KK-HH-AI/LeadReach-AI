package com.leadreach.user.controller;

import com.leadreach.user.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证接口：只做参数接收与响应组装，业务逻辑在 AuthService（service 包）
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // 发送短信验证码（模拟）
    @PostMapping("/send-code")
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body) {
        return authService.sendCode(body.get("phone"));
    }

    // 注册：手机号 + 验证码 + 密码 + 身份角色；姓名/邮箱选填
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        return authService.register(body.get("phone"), body.get("code"), body.get("password"),
                body.get("role"), body.get("name"), body.get("email"));
    }

    // 登录：手机号 + 密码，校验通过后签发 JWT
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        return authService.login(body.get("phone"), body.get("password"));
    }
}
