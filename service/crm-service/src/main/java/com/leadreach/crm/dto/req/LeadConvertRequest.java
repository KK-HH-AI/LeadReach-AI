package com.leadreach.crm.dto.req;

import lombok.Data;

/**
 * 线索转商机请求
 * 客户开发模块未就绪前，本模块只更新线索状态，不调用 RPC
 */
@Data
public class LeadConvertRequest {
    private String opportunityName;
    private java.math.BigDecimal amount;
    private String currency;
    /** 预计成交日期 */
    private String expectedCloseDate;
}