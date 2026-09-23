package com.leadreach.crm.dto.req;

import lombok.Data;

/**
 * 退回公海请求
 */
@Data
public class LeadRecycleRequest {
    /** 回收原因，写入分配日志 */
    private String reason;
}