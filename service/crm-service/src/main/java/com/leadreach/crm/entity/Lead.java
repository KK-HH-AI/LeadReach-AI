package com.leadreach.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索：公共池/私有池的载体
 * pool_type=PUBLIC 时 owner_user_id 为空；分配给销售后变 PRIVATE 并写 protected_until
 */
@Data
@TableName("lead")
public class Lead {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id)，租户隔离字段 */
    private Long tenantId;
    /** 采购商公司ID */
    private Long leadCompanyId;
    /** 主联系人 */
    private Long primaryContactId;
    private String title;
    private String sourceType;
    private Long sourceId;
    private String sourceUrl;
    /** NEW/ASSIGNED/CONTACTED/QUALIFIED/CONVERTED/INVALID */
    private String status;
    private String stage;
    private Integer score;
    /** A/B/C/D */
    private String grade;
    /** 负责人 user.id，NULL=公共池 */
    private Long ownerUserId;
    /** 负责团队 team.id */
    private Long ownerTeamId;
    /** PUBLIC/PRIVATE/PROTECTED */
    private String poolType;
    /** 保护期截止，超过后由定时任务退回公海 */
    private LocalDateTime protectedUntil;
    private Long convertedOpportunityId;
    private LocalDateTime convertedAt;
    /** 去重键 */
    private String dedupKey;
    private String remark;
    /** 创建人 user.id */
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}