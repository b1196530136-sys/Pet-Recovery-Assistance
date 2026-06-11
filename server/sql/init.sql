-- ============================================================
-- 流浪动物身份登记与寻宠互助平台 - 数据库初始化脚本
-- 数据库: pet_recovery
-- ============================================================

CREATE DATABASE IF NOT EXISTS pet_recovery DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pet_recovery;

-- ============================================================
-- 1. 用户主表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    email        VARCHAR(128) NOT NULL UNIQUE COMMENT '邮箱(登录账号)',
    password     VARCHAR(256) DEFAULT NULL COMMENT '密码(bcrypt加密)',
    nickname     VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    avatar       VARCHAR(512) DEFAULT NULL COMMENT '头像URL',
    phone        VARCHAR(256) DEFAULT NULL COMMENT '联系电话(加密存储)',
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色: USER/PENDING_CERT/CERTIFIED/ADMIN',
    cert_credentials VARCHAR(2048) DEFAULT NULL COMMENT '认证凭证URL(逗号分隔)',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用 1=正常',
    login_fail_count INT       NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    lock_time    DATETIME     DEFAULT NULL COMMENT '账号锁定截止时间',
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户主表';

-- ============================================================
-- 2. 寻宠启事主表
-- ============================================================
CREATE TABLE IF NOT EXISTS pet_search_post (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '启事ID',
    user_id       BIGINT        NOT NULL COMMENT '发布人ID',
    pet_type      VARCHAR(20)   NOT NULL COMMENT '宠物类型: cat/dog/other',
    breed         VARCHAR(64)   DEFAULT NULL COMMENT '品种',
    pet_name      VARCHAR(64)   DEFAULT NULL COMMENT '宠物昵称',
    photos        TEXT          DEFAULT NULL COMMENT '照片(JSON数组存储)',
    lost_time     DATETIME      DEFAULT NULL COMMENT '丢失时间',
    longitude     DECIMAL(10,7) DEFAULT NULL COMMENT '丢失经度(BD-09)',
    latitude      DECIMAL(10,7) DEFAULT NULL COMMENT '丢失纬度(BD-09)',
    address       VARCHAR(512)  DEFAULT NULL COMMENT '文字地址描述',
    reward        VARCHAR(256)  DEFAULT NULL COMMENT '酬金说明',
    description   TEXT          DEFAULT NULL COMMENT '特征描述',
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/ACTIVE/REJECTED/RESOLVED',
    reject_reason VARCHAR(512)  DEFAULT NULL COMMENT '驳回原因',
    photo_hash    VARCHAR(256)  DEFAULT NULL COMMENT '感知哈希值(逗号分隔,与photos对应)',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_lost_time (lost_time),
    INDEX idx_address (address(64))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='寻宠启事主表';

-- ============================================================
-- 3. 流浪动物电子档案表
-- ============================================================
CREATE TABLE IF NOT EXISTS stray_animal_archive (
    id               BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '档案ID',
    user_id          BIGINT        NOT NULL COMMENT '登记人ID(认证用户)',
    animal_type      VARCHAR(20)   NOT NULL COMMENT '动物大类: cat/dog/other',
    name             VARCHAR(64)   DEFAULT NULL COMMENT '动物昵称',
    longitude        DECIMAL(10,7) DEFAULT NULL COMMENT '发现经度(BD-09)',
    latitude         DECIMAL(10,7) DEFAULT NULL COMMENT '发现纬度(BD-09)',
    address          VARCHAR(512)  DEFAULT NULL COMMENT '文字地址',
    health_status    VARCHAR(64)   DEFAULT NULL COMMENT '健康状况评估',
    neutered_status  VARCHAR(64)   DEFAULT NULL COMMENT '绝育状态(已绝育/未绝育)',
    immune_status    VARCHAR(64)   DEFAULT NULL COMMENT '免疫状态(已免疫/未免疫)',
    placement_status VARCHAR(64)   DEFAULT NULL COMMENT '安置状态: observing/sheltered/adoptable/adopted',
    photos           TEXT          DEFAULT NULL COMMENT '现场照片(JSON数组)',
    description      TEXT          DEFAULT NULL COMMENT '备注描述',
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    reject_reason    VARCHAR(512)  DEFAULT NULL COMMENT '驳回原因',
    pending_data     TEXT          DEFAULT NULL COMMENT '待审核修改数据(JSON)',
    create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建档时间',
    update_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_placement (placement_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流浪动物电子档案表';

-- ============================================================
-- 4. 站内即时私信与线索表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_im_message (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    sender_id     BIGINT        NOT NULL COMMENT '发送方(线索提供人)ID',
    receiver_id   BIGINT        NOT NULL COMMENT '接收方(启事主人)ID',
    post_id       BIGINT        DEFAULT NULL COMMENT '关联启事ID',
    msg_type      TINYINT       NOT NULL DEFAULT 0 COMMENT '消息类型: 0=普通私信 1=线索 2=系统消息',
    content       TEXT          DEFAULT NULL COMMENT '消息内容/目击描述',
    clue_time     DATETIME      DEFAULT NULL COMMENT '目击时间(线索消息)',
    clue_longitude DECIMAL(10,7) DEFAULT NULL COMMENT '目击经度(线索消息)',
    clue_latitude  DECIMAL(10,7) DEFAULT NULL COMMENT '目击纬度(线索消息)',
    clue_address  VARCHAR(512)  DEFAULT NULL COMMENT '目击地址(线索消息)',
    clue_photos   TEXT          DEFAULT NULL COMMENT '目击照片(JSON数组)',
    read_status   TINYINT       NOT NULL DEFAULT 0 COMMENT '已读状态: 0=未读 1=已读',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_sender (sender_id),
    INDEX idx_receiver (receiver_id),
    INDEX idx_post (post_id),
    INDEX idx_read (receiver_id, read_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内即时私信与线索表';

-- ============================================================
-- 5. 验证码缓存表(可选,用于持久化验证码)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_verify_code (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT 'ID',
    email      VARCHAR(128) NOT NULL COMMENT '邮箱',
    code       VARCHAR(6)   NOT NULL COMMENT '6位验证码',
    expire_at  DATETIME     NOT NULL COMMENT '过期时间',
    used       TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已使用: 0=否 1=是',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    INDEX idx_email (email, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮箱验证码表';

-- ============================================================
-- 6. 领养申请表
-- ============================================================
CREATE TABLE IF NOT EXISTS adoption_request (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY COMMENT '申请ID',
    archive_id    BIGINT        NOT NULL COMMENT '关联档案ID',
    applicant_id  BIGINT        NOT NULL COMMENT '申请人ID',
    owner_id      BIGINT        NOT NULL COMMENT '档案发布人ID',
    message       TEXT          DEFAULT NULL COMMENT '申请留言',
    contact       VARCHAR(64)   DEFAULT NULL COMMENT '申请人联系方式',
    living_condition TEXT       DEFAULT NULL COMMENT '居住条件',
    pet_experience TEXT         DEFAULT NULL COMMENT '养宠经验',
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
    create_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    update_time   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_archive (archive_id),
    INDEX idx_applicant (applicant_id),
    INDEX idx_owner (owner_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领养申请表';

-- ============================================================
-- 7. 领养记录表
-- ============================================================
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

-- ============================================================
-- 8. 用户拉黑关系表
-- ============================================================
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

-- ============================================================
-- 9. 用户举报记录表
-- ============================================================
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

-- ============================================================
-- 初始化管理员账号(密码: admin123)
-- ============================================================
INSERT INTO sys_user (email, password, nickname, role, status)
VALUES ('admin@petrecovery.com',
        '$2b$10$JLvIuzkoeunleBDoQ.kdK.EQtpY9ffZrWDARMJzkEKfWxIFwjtjzi',
        '系统管理员', 'ADMIN', 1);
