package com.leadreach.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体：统一承载老板(BOSS)和员工(EMPLOYEE)两种身份
 * tenantId 为空 = 已注册但未加入任何公司（注册 → 拉人之间的中间态）
 */
@Data
@TableName("`user`")
public class User {
    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 手机号（登录账号），唯一 */
    private String phone;
    /** 姓名，选填（拉人检索用） */
    private String name;
    /** 邮箱，选填（拉人检索用） */
    private String email;
    /** 登录密码（BCrypt 哈希），注册时设置 */
    private String password;
    /** 身份角色：BOSS / EMPLOYEE */
    private String role;
    /** 所属公司ID(=company.id)，NULL=尚未加入公司 */
    private Long tenantId;
    /** 状态：1启用 0禁用 */
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
