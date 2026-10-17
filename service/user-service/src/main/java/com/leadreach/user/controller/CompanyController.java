package com.leadreach.user.controller;

import com.leadreach.user.service.CompanyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公司接口：只做身份解析与参数接收，业务逻辑在 CompanyService（service 包）
 */
@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    @Autowired
    private CompanyService companyService;

    // 创建公司：名称必填，行业/地区选填
    @PostMapping
    public Map<String, Object> create(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long bossId = currentUserId(request);
        if (bossId == null) {
            return Map.of("code", 1, "msg", "缺少用户身份");
        }
        return companyService.createCompany(bossId, body.get("name"), body.get("industry"), body.get("region"));
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
