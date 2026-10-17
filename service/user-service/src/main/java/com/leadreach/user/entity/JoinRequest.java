package com.leadreach.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工加入公司申请：员工提交(PENDING) → 老板同意(APPROVED，员工正式入公司) 或拒绝(REJECTED)
 * 本表跨租户使用，不参与租户插件自动过滤，查询条件由 Service 显式指定
 */
@Data
@TableName("join_request")
public class JoinRequest {
    /** 申请ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 目标公司ID(=company.id) */
    private Long tenantId;
    /** 申请人 user.id */
    private Long employeeId;
    /** 状态：PENDING / APPROVED / REJECTED */
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
