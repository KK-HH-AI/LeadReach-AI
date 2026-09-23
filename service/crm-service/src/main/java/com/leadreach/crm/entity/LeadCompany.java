package com.leadreach.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购商公司：线索的归属主体，按 normalized_name / domain 生成 dedup_hash 去重
 */
@Data
@TableName("lead_company")
public class LeadCompany {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id)，租户隔离字段 */
    private Long tenantId;
    private String name;
    /** 标准化公司名，用于去重 */
    private String normalizedName;
    /** 国家代码，如 DE、US */
    private String countryCode;
    /** 区域，如 EU、ASEAN、RU */
    private String regionCode;
    private String website;
    /** 域名，用于去重 */
    private String domain;
    private String industryCode;
    private String industryName;
    private String address;
    /** 来源：CUSTOMS/B2B/EXHIBITION/SOCIAL/LBS/BRAND/ECOMMERCE/MANUAL */
    private String sourceType;
    private Long sourceId;
    private String sourceUrl;
    /** 去重哈希 */
    private String dedupHash;
    /** ACTIVE/MERGED/DELETED */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}