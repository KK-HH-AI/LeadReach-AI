-- ============================================================
-- V5：员工申请加入公司
-- 流程：员工搜索公司 → 提交申请(PENDING) → 老板审批
--       同意(APPROVED)：员工入公司(默认团队/默认岗位) + 回填租户
--       拒绝(REJECTED)：仅标记，员工可另寻公司
-- 本表天然跨租户：写入方是尚无租户的员工，读取方是有租户的老板，
-- 故不纳入租户插件（见 MybatisPlusConfig ignoreTable），条件由 Service 显式控制。
-- ============================================================

CREATE TABLE `join_request` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL                COMMENT '目标公司ID(=company.id)',
    `employee_id` BIGINT UNSIGNED NOT NULL                COMMENT '申请人 user.id',
    `status`      VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING待审批 / APPROVED已同意 / REJECTED已拒绝',
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_employee` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工加入公司申请表';
