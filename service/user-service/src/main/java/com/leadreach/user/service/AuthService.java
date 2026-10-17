package com.leadreach.user.service;

import java.util.Map;

/**
 * 认证业务接口：短信验证码、注册（老板/员工）、密码登录
 */
public interface AuthService {

    /** 发送短信验证码（模拟） */
    Map<String, Object> sendCode(String phone);

    /** 注册：手机号 + 验证码 + 密码 + 身份角色；姓名/邮箱选填；注册时未加入任何公司（tenantId 为空） */
    Map<String, Object> register(String phone, String code, String password, String role, String name, String email);

    /** 登录：手机号 + 密码，校验通过后签发 JWT */
    Map<String, Object> login(String phone, String password);
}
