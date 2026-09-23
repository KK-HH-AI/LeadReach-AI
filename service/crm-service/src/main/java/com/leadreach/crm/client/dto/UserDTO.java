package com.leadreach.crm.client.dto;

import lombok.Data;

/**
 * user-service 内部接口返回的用户信息
 */
@Data
public class UserDTO {
    private Long id;
    private Long tenantId;
    /** 展示名（user 表暂无姓名字段，当前等于 phone） */
    private String name;
    private String phone;
    /** 所属团队 team.id */
    private Long teamId;
    /** 所属团队名称 */
    private String teamName;
    /** 岗位：BOSS / SALES_MANAGER / SALES_REP / OPERATOR */
    private String jobCode;
    /** 1启用 0禁用 */
    private Integer status;
}