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

ALTER TABLE sys_user
    MODIFY COLUMN phone VARCHAR(256) DEFAULT NULL COMMENT '联系电话(加密存储)';

ALTER TABLE stray_animal_archive
    ADD COLUMN pending_data TEXT DEFAULT NULL COMMENT '待审核修改数据(JSON)' AFTER reject_reason;

ALTER TABLE sys_im_message
    MODIFY COLUMN msg_type TINYINT NOT NULL DEFAULT 0 COMMENT '消息类型: 0=普通私信 1=线索 2=系统消息';

ALTER TABLE adoption_request
    ADD COLUMN contact VARCHAR(64) DEFAULT NULL COMMENT '申请人联系方式' AFTER message,
    ADD COLUMN living_condition TEXT DEFAULT NULL COMMENT '居住条件' AFTER contact,
    ADD COLUMN pet_experience TEXT DEFAULT NULL COMMENT '养宠经验' AFTER living_condition;

CREATE TABLE IF NOT EXISTS adoption_record (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    request_id      BIGINT       NOT NULL COMMENT '领养申请ID',
    archive_id      BIGINT       NOT NULL COMMENT '关联档案ID',
    adopter_id      BIGINT       NOT NULL COMMENT '领养人ID',
    owner_id        BIGINT       NOT NULL COMMENT '档案发布人ID',
    contact         VARCHAR(64)  DEFAULT NULL COMMENT '申请人联系方式',
    living_condition TEXT        DEFAULT NULL COMMENT '居住条件',
    pet_experience  TEXT         DEFAULT NULL COMMENT '养宠经验',
    follow_up_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '回访状态: PENDING/VISITED/ABNORMAL',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_request (request_id),
    INDEX idx_archive (archive_id),
    INDEX idx_adopter (adopter_id),
    INDEX idx_owner (owner_id),
    INDEX idx_follow_up_status (follow_up_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领养记录表';

INSERT INTO adoption_record (
    request_id, archive_id, adopter_id, owner_id, contact, living_condition, pet_experience,
    follow_up_status, create_time, update_time
)
SELECT
    id, archive_id, applicant_id, owner_id, contact, living_condition, pet_experience,
    'PENDING', create_time, update_time
FROM adoption_request
WHERE status = 'APPROVED'
ON DUPLICATE KEY UPDATE request_id = request_id;

CREATE TABLE IF NOT EXISTS sys_user_block (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '关系ID',
    user_id         BIGINT       NOT NULL COMMENT '执行拉黑的用户ID',
    blocked_user_id BIGINT       NOT NULL COMMENT '被拉黑用户ID',
    reason          VARCHAR(255) DEFAULT NULL COMMENT '拉黑原因',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
    UNIQUE KEY uk_user_block (user_id, blocked_user_id),
    INDEX idx_user_blocked (user_id, blocked_user_id),
    INDEX idx_blocked_user (blocked_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户拉黑关系表';

CREATE TABLE IF NOT EXISTS sys_user_report (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '举报ID',
    reporter_id      BIGINT       NOT NULL COMMENT '举报人ID',
    reported_user_id BIGINT       NOT NULL COMMENT '被举报用户ID',
    message_id       BIGINT       DEFAULT NULL COMMENT '关联消息ID',
    report_type      VARCHAR(32)  NOT NULL COMMENT '举报类型: SPAM/HARASSMENT/FALSE_CLUE/OTHER',
    reason           VARCHAR(128) DEFAULT NULL COMMENT '举报原因摘要',
    detail           TEXT         DEFAULT NULL COMMENT '补充说明',
    message_snapshot TEXT         DEFAULT NULL COMMENT '举报时的消息快照',
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '处理状态: PENDING/RESOLVED/REJECTED',
    handle_note      VARCHAR(512) DEFAULT NULL COMMENT '处理备注',
    handled_by       BIGINT       DEFAULT NULL COMMENT '处理管理员ID',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_reporter_status (reporter_id, status),
    INDEX idx_reported_status (reported_user_id, status),
    INDEX idx_report_message (message_id),
    INDEX idx_report_status_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户举报记录表';
