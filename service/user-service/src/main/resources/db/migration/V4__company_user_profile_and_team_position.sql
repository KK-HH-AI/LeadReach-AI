-- ============================================================
-- V4：公司资料补全 + 用户姓名邮箱 + 团队自定义岗位类型
-- 1) company 补行业/地区（name V1 已有，凑齐需求"名称、行业、地区"）
-- 2) user 补姓名/邮箱：老板拉人可按 姓名/邮箱/手机 多种方式检索
-- 3) 团队岗位类型：老板先在团队下定义岗位（如 邮件运营/采集专员），
--    再把成员按岗位类型分类（boss_employee.team_position_id 记录归属）。
--    权限岗位(position 枚举)保持不变，数据权限逻辑不受影响。
-- ============================================================

-- 1. 公司资料补全
ALTER TABLE `company`
    ADD COLUMN `industry` VARCHAR(50)  NULL DEFAULT NULL COMMENT '行业' AFTER `name`,
    ADD COLUMN `region`   VARCHAR(50)  NULL DEFAULT NULL COMMENT '地区' AFTER `industry`;

-- 2. 用户资料补全（选填，搜索检索用）
ALTER TABLE `user`
    ADD COLUMN `name`  VARCHAR(50)   NULL DEFAULT NULL COMMENT '姓名' AFTER `phone`,
    ADD COLUMN `email` VARCHAR(100)  NULL DEFAULT NULL COMMENT '邮箱' AFTER `name`,
    ADD KEY `idx_name` (`name`),
    ADD KEY `idx_email` (`email`);

-- 3. 团队自定义岗位类型表
--    岗位类型是团队级配置数据，删除采用物理删除（避免 (team_id,name) 唯一键与软删记录冲突）
CREATE TABLE `team_position` (
    `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '岗位类型ID',
    `tenant_id`  BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `team_id`    BIGINT UNSIGNED NOT NULL                COMMENT '所属团队 team.id',
    `name`       VARCHAR(50)     NOT NULL                COMMENT '岗位类型名称，如：邮件运营/采集专员',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_team_name` (`team_id`, `name`),
    KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队自定义岗位类型表';

-- 4. 成员挂团队岗位类型（NULL=未分类）
ALTER TABLE `boss_employee`
    ADD COLUMN `team_position_id` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '团队自定义岗位 team_position.id' AFTER `team_id`,
    ADD KEY `idx_team_position_id` (`team_position_id`);
