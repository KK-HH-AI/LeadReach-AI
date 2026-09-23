package com.leadreach.crm.dto.req;

import lombok.Data;

/**
 * 分配线索请求
 */
@Data
public class LeadAssignRequest {
    /** 目标销售 user.id */
    private Long toUserId;
    /** 分配原因，写入分配日志 */
    private String reason;
}