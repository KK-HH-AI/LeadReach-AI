package com.leadreach.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 联系人 / 关键人（KP）：隶属于采购商公司
 */
@Data
@TableName("contact")
public class Contact {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id)，租户隔离字段 */
    private Long tenantId;
    /** 所属采购商公司 */
    private Long leadCompanyId;
    private String name;
    private String firstName;
    private String lastName;
    private String jobTitle;
    private String department;
    /** 职位层级：C_LEVEL/DIRECTOR/MANAGER/STAFF */
    private String seniority;
    private String email;
    /** VALID/INVALID/UNKNOWN */
    private String emailStatus;
    private String phone;
    private String whatsapp;
    private String linkedinUrl;
    private String facebookUrl;
    private String countryCode;
    private String language;
    /** 是否关键人：1是 0否 */
    private Integer isKp;
    private String dedupHash;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}