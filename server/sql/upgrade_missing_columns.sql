-- ============================================================
-- Existing database upgrade: fields required before test phase
-- Database: pet_recovery
-- Run this once if the database was created before init.sql was updated.
-- ============================================================

USE pet_recovery;

ALTER TABLE sys_user
    ADD COLUMN cert_credentials VARCHAR(2048) DEFAULT NULL COMMENT '认证凭证URL(逗号分隔)' AFTER role,
    ADD COLUMN login_fail_count INT NOT NULL DEFAULT 0 COMMENT '连续登录失败次数' AFTER status,
    ADD COLUMN lock_time DATETIME DEFAULT NULL COMMENT '账号锁定截止时间' AFTER login_fail_count;

ALTER TABLE stray_animal_archive
    ADD COLUMN pending_data TEXT DEFAULT NULL COMMENT '待审核修改数据(JSON)' AFTER reject_reason;

ALTER TABLE sys_im_message
    MODIFY COLUMN msg_type TINYINT NOT NULL DEFAULT 0 COMMENT '消息类型: 0=普通私信 1=线索';
