package com.leadreach.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 老板-员工团队关系：员工归属关系 + 岗位，都在这一张表维护
 */
@Data
@TableName("boss_employee")
public class BossEmployee {
    /** 关系ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id) */
    private Long tenantId;
    /** 老板用户ID(user.id) */
    private Long bossId;
    /** 员工用户ID(user.id) */
    private Long employeeId;
    /** 岗位：SALES_REP / SALES_MANAGER / OPERATOR */
    private String position;
    /** 所属团队 team.id，NULL=尚未分配到具体团队 */
    private Long teamId;
    /** 状态：1在职 0已移除 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
