package com.leadreach.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跟进记录：biz_type + biz_id 通用挂载，本期只用于 LEAD
 */
@Data
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id)，租户隔离字段 */
    private Long tenantId;
    /** LEAD/OPPORTUNITY/CUSTOMER */
    private String bizType;
    private Long bizId;
    /** CALL/EMAIL/WHATSAPP/LINKEDIN/MEETING/NOTE */
    private String activityType;
    private String content;
    private String result;
    /** 跟进人 user.id */
    private Long ownerUserId;
    private LocalDateTime activityAt;
    /** 下次跟进时间 */
    private LocalDateTime nextFollowAt;
    private LocalDateTime createdAt;
}