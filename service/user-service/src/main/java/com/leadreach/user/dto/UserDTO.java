package com.leadreach.user.dto;

import lombok.Data;

/**
 * 内部接口返回的用户信息（供跨服务调用）
 * 注意：user 表当前没有姓名字段，name 暂以手机号回填，前端展示口径不变
 */
@Data
public class UserDTO {
    /** 用户ID */
    private Long id;
    /** 所属公司ID(=company.id) */
    private Long tenantId;
    /** 展示名（暂无姓名列，暂等于 phone） */
    private String name;
    /** 手机号 */
    private String phone;
    /** 所属团队 team.id */
    private Long teamId;
    /** 所属团队名称 */
    private String teamName;
    /** 岗位：BOSS / SALES_MANAGER / SALES_REP / OPERATOR */
    private String jobCode;
    /** 状态：1启用 0禁用 */
    private Integer status;
}