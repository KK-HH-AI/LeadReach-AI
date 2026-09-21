-- ============================================================
-- V1：用户模块初始化
-- 身份角色：BOSS(老板) / EMPLOYEE(员工)
-- 岗位：SALES_REP(销售专员) / SALES_MANAGER(销售主管) / OPERATOR(运营)
-- 多租户：表级隔离，tenant_id 即 company.id
--         company 表是租户根表，不参与租户条件过滤
-- ============================================================

-- 1. 用户表：统一承载老板和员工账号
CREATE TABLE `user` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `phone`      VARCHAR(20)     NOT NULL                COMMENT '手机号（登录账号）',
    `role`       VARCHAR(20)     NOT NULL                COMMENT '身份角色：BOSS / EMPLOYEE',
    `tenant_id`  BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '所属公司ID(=company.id)，NULL=尚未加入公司',
    `status`     TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1启用 0禁用',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 2. 公司表：公司ID即租户ID，直接持有老板（1公司1老板）
CREATE TABLE `company` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公司ID（即租户ID）',
    `name`       VARCHAR(100)    NOT NULL                COMMENT '公司名称',
    `boss_id`    BIGINT UNSIGNED NOT NULL                COMMENT '老板用户ID(user.id)',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_boss_id` (`boss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公司表';

-- 3. 老板-员工团队关系表：唯一的员工归属关系（岗位在此维护）
CREATE TABLE `boss_employee` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '关系ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `boss_id`     BIGINT UNSIGNED NOT NULL                COMMENT '老板用户ID(user.id)',
    `employee_id` BIGINT UNSIGNED NOT NULL                COMMENT '员工用户ID(user.id)',
    `position`    VARCHAR(20)     NOT NULL                COMMENT '岗位：SALES_REP/SALES_MANAGER/OPERATOR',
    `status`      TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1在职 0已移除',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_boss_employee` (`boss_id`, `employee_id`),
    KEY `idx_tenant_id` (`tenant_id`),
    KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='老板-员工团队关系表';

-- 4. 员工内容分配表（占位设计：内容类型暂未定，先建通用字段，V2 再细化）
CREATE TABLE `content_assignment` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分配ID',
    `tenant_id`    BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `boss_id`      BIGINT UNSIGNED NOT NULL                COMMENT '分配人老板ID(user.id)',
    `employee_id`  BIGINT UNSIGNED NOT NULL                COMMENT '被分配员工ID(user.id)',
    `content_type` VARCHAR(50)     NOT NULL                COMMENT '内容类型（预留：客户/线索/物料等）',
    `content_id`   BIGINT UNSIGNED NOT NULL                COMMENT '内容ID（预留，指向业务资源）',
    `status`       TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1有效 0已撤销',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_employee` (`tenant_id`, `employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工内容分配表';
