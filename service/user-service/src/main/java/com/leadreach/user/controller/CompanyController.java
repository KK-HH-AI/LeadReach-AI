package com.leadreach.user.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.leadreach.user.entity.Company;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.CompanyMapper;
import com.leadreach.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公司业务：老板先注册账号，再创建公司（公司ID即租户ID）
 */
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private UserMapper userMapper;

    // 创建公司
    @PostMapping
    @Transactional
    public Map<String, Object> create(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long bossId = currentUserId(request);
        if (bossId == null) {
            return Map.of("code", 1, "msg", "缺少用户身份");
        }
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            return Map.of("code", 1, "msg", "公司名称不能为空");
        }

        // 1. 必须是老板角色
        User boss = userMapper.selectById(bossId);
        if (boss == null || !"BOSS".equals(boss.getRole())) {
            return Map.of("code", 1, "msg", "只有老板才能创建公司");
        }
        // 2. 一个老板只能有一个公司
        if (boss.getTenantId() != null) {
            return Map.of("code", 1, "msg", "该老板已创建过公司");
        }

        // 3. 创建公司（ID 即租户ID）
        Company company = new Company();
        company.setName(name);
        company.setBossId(bossId);
        companyMapper.insert(company);

        // 4. 回填：老板获得租户上下文
        boss.setTenantId(company.getId());
        userMapper.updateById(boss);

        Map<String, Object> data = new HashMap<>();
        data.put("companyId", company.getId());
        data.put("name", company.getName());
        return Map.of("code", 0, "msg", "创建公司成功", "data", data);
    }

    // 从网关透传的 X-User-Id 请求头取当前用户ID
    private Long currentUserId(HttpServletRequest request) {
        String uid = request.getHeader("X-User-Id");
        if (uid == null || uid.isBlank()) {
            return null;
        }
        return Long.parseLong(uid.trim());
    }
}
