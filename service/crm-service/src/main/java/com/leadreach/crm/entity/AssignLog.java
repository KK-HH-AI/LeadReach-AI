package com.leadreach.crm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分配/领取/回收日志：线索归属变更的可追溯记录
 */
@Data
@TableName("assign_log")
public class AssignLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id)，租户隔离字段 */
    private Long tenantId;
    private Long leadId;
    /** ASSIGN/CLAIM/TRANSFER/RECYCLE */
    private String action;
    private Long fromOwnerUserId;
    private Long toOwnerUserId;
    private Long fromTeamId;
    private Long toTeamId;
    private String reason;
    /** 操作人 user.id */
    private Long operatorId;
    private LocalDateTime createdAt;
}