package com.leadreach.crm.client.dto;

import lombok.Data;

/**
 * user-service 内部接口返回的团队信息
 */
@Data
public class TeamDTO {
    private Long id;
    private Long tenantId;
    private String name;
    /** 团队负责人(销售主管) user.id */
    private Long leaderUserId;
    private String leaderPhone;
    /** 1默认团队 0普通团队 */
    private Integer isDefault;
    private Integer status;
}