package com.leadreach.user.service.impl;

import com.leadreach.user.entity.Company;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.CompanyMapper;
import com.leadreach.user.mapper.UserMapper;
import com.leadreach.user.service.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 公司业务实现：老板先注册账号，再创建公司（公司ID即租户ID）
 */
@Service
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private UserMapper userMapper;

    // 创建公司
    @Override
    @Transactional
    public Map<String, Object> createCompany(Long bossId, String name, String industry, String region) {
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

        // 3. 创建公司（ID 即租户ID；行业/地区选填）
        Company company = new Company();
        company.setName(name.trim());
        if (industry != null && !industry.isBlank()) {
            company.setIndustry(industry.trim());
        }
        if (region != null && !region.isBlank()) {
            company.setRegion(region.trim());
        }
        company.setBossId(bossId);
        companyMapper.insert(company);

        // 4. 回填：老板获得租户上下文
        boss.setTenantId(company.getId());
        userMapper.updateById(boss);

        Map<String, Object> data = new HashMap<>();
        data.put("companyId", company.getId());
        data.put("name", company.getName());
        data.put("industry", company.getIndustry());
        data.put("region", company.getRegion());
        return Map.of("code", 0, "msg", "创建公司成功", "data", data);
    }
}
