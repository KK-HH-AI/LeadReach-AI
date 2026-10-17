package com.leadreach.user.service;

import java.util.Map;

/**
 * 公司业务接口：老板先注册账号，再创建公司（公司ID即租户ID）
 */
public interface CompanyService {

    /** 创建公司：校验老板身份与一人一公司，创建后回填老板的 tenantId；行业/地区选填 */
    Map<String, Object> createCompany(Long bossId, String name, String industry, String region);
}
