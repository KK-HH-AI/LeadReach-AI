package com.leadreach.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司实体：公司ID即租户ID，直接持有老板（1公司1老板）
 */
@Data
@TableName("company")
public class Company {
    /** 公司ID（即租户ID） */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 公司名称 */
    private String name;
    /** 行业，选填 */
    private String industry;
    /** 地区，选填 */
    private String region;
    /** 老板用户ID(user.id) */
    private Long bossId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
