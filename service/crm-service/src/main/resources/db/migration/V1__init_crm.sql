-- ============================================================
-- V1：线索模块初始化（crm_db 专属库）
-- 隔离原则：
--   1. 所有表带 tenant_id(=company.id)，由 TenantLineInnerInterceptor 自动拼租户条件
--   2. 不建物理外键，跨服务只用逻辑 ID（owner_user_id → user_db.user.id）
--   3. deleted_at 为预留字段，本期不做物理删除/逻辑删除
-- ============================================================

-- 1. 采购商公司
CREATE TABLE `lead_company` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公司ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `name`            VARCHAR(255)    NOT NULL                COMMENT '公司名',
    `normalized_name` VARCHAR(255)    NULL DEFAULT NULL       COMMENT '标准化公司名，用于去重',
    `country_code`    VARCHAR(8)      NULL DEFAULT NULL       COMMENT '国家代码，如 DE、US',
    `region_code`     VARCHAR(32)     NULL DEFAULT NULL       COMMENT '区域，如 EU、ASEAN、RU',
    `website`         VARCHAR(255)    NULL DEFAULT NULL,
    `domain`          VARCHAR(128)    NULL DEFAULT NULL       COMMENT '域名，用于去重',
    `industry_code`   VARCHAR(64)     NULL DEFAULT NULL,
    `industry_name`   VARCHAR(128)    NULL DEFAULT NULL,
    `address`         VARCHAR(512)    NULL DEFAULT NULL,
    `source_type`     VARCHAR(32)     NULL DEFAULT NULL       COMMENT '来源：CUSTOMS/B2B/EXHIBITION/SOCIAL/LBS/BRAND/ECOMMERCE/MANUAL',
    `source_id`       BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '来源记录ID，接采集模块后使用',
    `source_url`      VARCHAR(512)    NULL DEFAULT NULL,
    `dedup_hash`      VARCHAR(64)     NULL DEFAULT NULL       COMMENT '去重哈希',
    `status`          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/MERGED/DELETED',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`      DATETIME        NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_dedup` (`tenant_id`, `dedup_hash`),
    KEY `idx_tenant_name` (`tenant_id`, `normalized_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购商公司';

-- 2. 联系人 / KP
CREATE TABLE `contact` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
    `tenant_id`       BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `lead_company_id` BIGINT UNSIGNED NOT NULL                COMMENT '所属采购商公司',
    `name`            VARCHAR(128)    NULL DEFAULT NULL,
    `first_name`      VARCHAR(64)     NULL DEFAULT NULL,
    `last_name`       VARCHAR(64)     NULL DEFAULT NULL,
    `job_title`       VARCHAR(128)    NULL DEFAULT NULL,
    `department`      VARCHAR(64)     NULL DEFAULT NULL,
    `seniority`       VARCHAR(32)     NULL DEFAULT NULL       COMMENT '职位层级：C_LEVEL/DIRECTOR/MANAGER/STAFF',
    `email`           VARCHAR(255)    NULL DEFAULT NULL,
    `email_status`    VARCHAR(16)     NULL DEFAULT NULL       COMMENT 'VALID/INVALID/UNKNOWN',
    `phone`           VARCHAR(64)     NULL DEFAULT NULL,
    `whatsapp`        VARCHAR(64)     NULL DEFAULT NULL,
    `linkedin_url`    VARCHAR(512)    NULL DEFAULT NULL,
    `facebook_url`    VARCHAR(512)    NULL DEFAULT NULL,
    `country_code`    VARCHAR(8)      NULL DEFAULT NULL,
    `language`        VARCHAR(16)     NULL DEFAULT NULL,
    `is_kp`           TINYINT         NOT NULL DEFAULT 0      COMMENT '是否关键人',
    `dedup_hash`      VARCHAR(64)     NULL DEFAULT NULL,
    `status`          VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE',
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`      DATETIME        NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_company` (`tenant_id`, `lead_company_id`),
    KEY `idx_tenant_email` (`tenant_id`, `email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='联系人/KP';

-- 3. 线索
CREATE TABLE `lead` (
    `id`                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '线索ID',
    `tenant_id`                BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `lead_company_id`          BIGINT UNSIGNED NOT NULL                COMMENT '采购商公司ID',
    `primary_contact_id`       BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '主联系人',
    `title`                    VARCHAR(255)    NULL DEFAULT NULL       COMMENT '线索标题',
    `source_type`              VARCHAR(32)     NULL DEFAULT NULL       COMMENT '来源渠道',
    `source_id`                BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '来源记录ID',
    `source_url`               VARCHAR(512)    NULL DEFAULT NULL,
    `status`                   VARCHAR(16)     NOT NULL DEFAULT 'NEW'
        COMMENT 'NEW/ASSIGNED/CONTACTED/QUALIFIED/CONVERTED/INVALID',
    `stage`                    VARCHAR(32)     NULL DEFAULT NULL       COMMENT '阶段，可扩展',
    `score`                    INT             NULL DEFAULT 0,
    `grade`                    VARCHAR(8)      NULL DEFAULT NULL       COMMENT 'A/B/C/D',
    `owner_user_id`            BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '负责人 user.id',
    `owner_team_id`            BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '负责团队 team.id',
    `pool_type`                VARCHAR(16)     NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PRIVATE/PROTECTED',
    `protected_until`          DATETIME        NULL DEFAULT NULL       COMMENT '保护期截止',
    `converted_opportunity_id` BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '转商机后关联',
    `converted_at`             DATETIME        NULL DEFAULT NULL,
    `dedup_key`                VARCHAR(128)    NULL DEFAULT NULL       COMMENT '去重键',
    `remark`                   VARCHAR(512)    NULL DEFAULT NULL,
    `created_by`               BIGINT UNSIGNED NULL DEFAULT NULL       COMMENT '创建人 user.id',
    `created_at`               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`               DATETIME        NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_pool` (`tenant_id`, `pool_type`, `owner_user_id`),
    KEY `idx_tenant_owner` (`tenant_id`, `owner_user_id`, `status`),
    KEY `idx_tenant_team` (`tenant_id`, `owner_team_id`, `status`),
    KEY `idx_tenant_dedup` (`tenant_id`, `dedup_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='线索';

-- 4. 跟进记录（biz_type 通用化，后续可挂商机/客户）
CREATE TABLE `activity` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '跟进记录ID',
    `tenant_id`      BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `biz_type`       VARCHAR(16)     NOT NULL                COMMENT 'LEAD/OPPORTUNITY/CUSTOMER',
    `biz_id`         BIGINT UNSIGNED NOT NULL                COMMENT '业务ID',
    `activity_type`  VARCHAR(32)     NOT NULL                COMMENT 'CALL/EMAIL/WHATSAPP/LINKEDIN/MEETING/NOTE',
    `content`        VARCHAR(2048)   NULL DEFAULT NULL,
    `result`         VARCHAR(512)    NULL DEFAULT NULL,
    `owner_user_id`  BIGINT UNSIGNED NOT NULL                COMMENT '跟进人 user.id',
    `activity_at`    DATETIME        NOT NULL                COMMENT '跟进时间',
    `next_follow_at` DATETIME        NULL DEFAULT NULL       COMMENT '下次跟进时间',
    `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_biz` (`tenant_id`, `biz_type`, `biz_id`, `activity_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='跟进记录';

-- 5. 分配/领取/回收日志
CREATE TABLE `assign_log` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `tenant_id`          BIGINT UNSIGNED NOT NULL                COMMENT '公司ID(=company.id)',
    `lead_id`            BIGINT UNSIGNED NOT NULL                COMMENT '线索ID',
    `action`             VARCHAR(16)     NOT NULL                COMMENT 'ASSIGN/CLAIM/TRANSFER/RECYCLE',
    `from_owner_user_id` BIGINT UNSIGNED NULL DEFAULT NULL,
    `to_owner_user_id`   BIGINT UNSIGNED NULL DEFAULT NULL,
    `from_team_id`       BIGINT UNSIGNED NULL DEFAULT NULL,
    `to_team_id`         BIGINT UNSIGNED NULL DEFAULT NULL,
    `reason`             VARCHAR(255)    NULL DEFAULT NULL,
    `operator_id`        BIGINT UNSIGNED NOT NULL                COMMENT '操作人 user.id',
    `created_at`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_tenant_lead` (`tenant_id`, `lead_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分配/领取/回收日志';