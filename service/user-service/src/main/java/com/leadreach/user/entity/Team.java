package com.leadreach.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团队：一个公司(租户)下可建多个团队，由销售主管带队
 * tenantId 即 company.id，团队是轮廓线模块 TEAM 级数据权限的依据
 */
@Data
@TableName("team")
public class Team {
    /** 团队ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id) */
    private Long tenantId;
    /** 团队名称 */
    private String name;
    /** 团队负责人(销售主管) user.id */
    private Long leaderUserId;
    /** 是否默认团队：1是 0否 */
    private Integer isDefault;
    /** 状态：1启用 0停用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}