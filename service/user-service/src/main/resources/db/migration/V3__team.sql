-- ============================================================
-- V3：团队表 + 员工团队归属
-- 背景：V1 里没有独立团队表，"团队"等价于公司（teamId = tenantId），
--       导致销售主管与老板的数据范围完全相同，线索模块无法做 TEAM 级数据权限。
-- 本版引入真正的团队：一个公司可建多个团队，团队由销售主管(SALES_MANAGER)带队。
-- tenant_id 仍为 company.id，租户隔离规则不变。
-- ============================================================

-- 1. 团队表
CREATE TABLE `team` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '团队ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `name`           VARCHAR(50)     NOT NULL                COMMENT '团队名称',
    `leader_user_id` BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '团队负责人(销售主管) user.id',
    `is_default`     TINYINT         NOT NULL DEFAULT 0      COMMENT '是否默认团队：1是 0否（公司创建时自动生成）',
    `status`         TINYINT         NOT NULL DEFAULT 1      COMMENT '状态：1启用 0停用',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_name` (`tenant_id`, `name`),
    KEY `idx_tenant` (`tenant_id`, `status`),
    KEY `idx_leader` (`leader_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队表';

-- 2. 员工表增加团队归属（NULL = 尚未分配到具体团队）
ALTER TABLE `boss_employee`
    ADD COLUMN `team_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '所属团队 team.id' AFTER `position`,
    ADD KEY `idx_team_id` (`team_id`);