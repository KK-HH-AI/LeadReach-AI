-- ============================================================
-- V2：用户表增加登录密码列（认证改为手机号+密码）
-- 密码存储使用 BCrypt 哈希（cn.hutool.crypto.digest.BCrypt）
-- 存量用户密码为空，需重新注册；V1 引入的旧测试数据建议清空后走新流程
-- ============================================================

ALTER TABLE `user`
    ADD COLUMN `password` VARCHAR(64) NULL DEFAULT NULL COMMENT '登录密码（BCrypt 哈希）' AFTER `phone`;
