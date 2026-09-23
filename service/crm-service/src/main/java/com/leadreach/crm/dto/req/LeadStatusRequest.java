package com.leadreach.crm.dto.req;

import lombok.Data;

/**
 * 更新线索状态请求
 */
@Data
public class LeadStatusRequest {
    /** 目标状态：CONTACTED / QUALIFIED / INVALID */
    private String status;
    private String remark;
}