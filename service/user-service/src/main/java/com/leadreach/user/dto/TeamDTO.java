package com.leadreach.user.dto;

import lombok.Data;

/**
 * 内部接口返回的团队信息（供跨服务调用）
 */
@Data
public class TeamDTO {
    /** 团队ID */
    private Long id;
    /** 所属公司ID(=company.id) */
    private Long tenantId;
    /** 团队名称 */
    private String name;
    /** 团队负责人(销售主管) user.id */
    private Long leaderUserId;
    /** 负责人手机号 */
    private String leaderPhone;
    /** 是否默认团队：1是 0否 */
    private Integer isDefault;
    /** 状态：1启用 0停用 */
    private Integer status;
}