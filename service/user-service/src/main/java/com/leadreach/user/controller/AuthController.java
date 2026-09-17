package com.leadreach.user.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/auth")
// 认证业务类
public class AuthController {

    @Autowired
    // 声明一个成员变量，变量名叫redisTemplate，类型是StringRedisTemplate
    private StringRedisTemplate redisTemplate;

    // 一个方法
    // 发送短信验证码功能
    @PostMapping("/send-code")
    // 请求发过来的是一段JSON，@RequestBody Map<String, String> body 是一个整体，意思是
    // @RequestBody告诉Spring从HTTP请求体中拿数据然后转成Map<String, String>的java类型，然后把这个值放到body的变量里
    public Map<String, Object> sendCode(@RequestBody Map<String, String> body) {
        // 获取前端传入的手机号
        String phone = body.get("phone");

        // 生成验证号码
        String code = RandomUtil.randomNumbers(6);

        // 将手机号和验证码存入redis
        // 键的命名”sms:code:phone“
        // 值：验证码
        // 保存在服务器中，5分钟后过期
        redisTemplate.opsForValue().set("sms:code" + phone, code, 5, TimeUnit.MINUTES);

        // 测试用，控制台打印短信
        System.out.println("【模拟短信】手机号：" + phone + ", 验证码：" + code);

        // 返回响应
        return Map.of("code", 0, "msg", "验证码已发送");
    }

    @PostMapping("/login")
    // 验证码登录功能
    public Map<String, Object> login(@RequestBody Map<String, String> body) {

        // 拿出手机号和验证码
        String phone = body.get("phone");
        String code = body.get("code");

        // 从redis中查询该手机号对应的缓存验证码
        String cached = redisTemplate.opsForValue().get("sms:code" + phone);

        // 校验
        if (cached == null || !cached.equals(code)){
            return Map.of("code", 1, "msg", "验证码错误或已经过期");
        }

        // 校验通过后，生成jwt令牌
        // 使用一个数据结构来存储用户的数据，我是用hashmap
        Map<String, Object> payload = new HashMap<>();
        // 保存用户电话信息
        payload.put("phone", phone);
        // 设置用户信息过期时间,未来的某一个时间戳
        payload.put("expire", System.currentTimeMillis() + 24 * 3600 * 1000);
        // 使用用户信息创建一个令牌
        // 密钥的计算逻辑：后端校验 token 时：拿收到 token 的【第一段 + 第二段】+ 同一个密钥，**重新计算签名**，
        // 和传过来的第三段对比。一致代表没被篡改；不一致直接拒绝。
        // 这里的密钥就是”your-secret-key“
        String token = JWTUtil.createToken(payload, "your-secret-key".getBytes());

        // 删除验证码，因为验证码是一次性的
        redisTemplate.delete("sms:code:" + phone);

        // 组装返回给前端的登录数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("phone", phone);
        return Map.of("code", 0, "msg", "登录成功", "data", data);
    }

}
