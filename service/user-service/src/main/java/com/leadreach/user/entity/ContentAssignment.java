package com.leadreach.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工内容分配实体：老板给员工分配负责的内容（内容类型暂未定，先通用化）
 * tenant_id 由多租户插件自动填充
 */
@Data
@TableName("content_assignment")
public class ContentAssignment {
    /** 分配ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司ID(=company.id) */
    private Long tenantId;
    /** 分配人老板ID(user.id) */
    private Long bossId;
    /** 被分配员工ID(user.id) */
    private Long employeeId;
    /** 内容类型（预留：客户/线索/物料等） */
    private String contentType;
    /** 内容ID（预留，指向业务资源） */
    private Long contentId;
    /** 状态：1有效 0已撤销 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
