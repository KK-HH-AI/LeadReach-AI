package com.leadreach.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 团队自定义岗位类型：老板先在团队下定义岗位（如 邮件运营/采集专员），
 * 再把成员按岗位类型分类。权限岗位（boss_employee.position 枚举）与此独立。
 */
@Data
@TableName("team_position")
public class TeamPosition {
    /** 岗位类型ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id) */
    private Long tenantId;
    /** 所属团队 team.id */
    private Long teamId;
    /** 岗位类型名称 */
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
