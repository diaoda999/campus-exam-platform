-- ============================================================
-- 校内在线考试平台 建表脚本  campus_exam
-- MySQL 8.0 / InnoDB / utf8mb4
-- ============================================================
CREATE DATABASE IF NOT EXISTS campus_exam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_exam;

-- ---------- 用户与班级 ----------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(64)  NOT NULL COMMENT '登录名',
    password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 密文',
    real_name   VARCHAR(64)           DEFAULT NULL COMMENT '姓名',
    role        VARCHAR(16)  NOT NULL COMMENT 'ADMIN/TEACHER/STUDENT',
    class_id    BIGINT                DEFAULT NULL COMMENT '学生所属班级',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_user_class (class_id)
) ENGINE = InnoDB COMMENT ='用户表';

DROP TABLE IF EXISTS sys_class;
CREATE TABLE sys_class (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    class_name  VARCHAR(64) NOT NULL,
    teacher_id  BIGINT               DEFAULT NULL,
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='班级表';

-- ---------- 知识点 ----------
DROP TABLE IF EXISTS knowledge_point;
CREATE TABLE knowledge_point (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(64) NOT NULL,
    parent_id   BIGINT      NOT NULL DEFAULT 0,
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='知识点';

-- ---------- 题库 ----------
DROP TABLE IF EXISTS question;
CREATE TABLE question (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    type        TINYINT      NOT NULL COMMENT '1单选2多选3判断4填空5简答',
    stem        TEXT         NOT NULL COMMENT '题干',
    options     TEXT                  DEFAULT NULL COMMENT '选项JSON',
    answer      TEXT                  DEFAULT NULL COMMENT '标准答案',
    analysis    TEXT                  DEFAULT NULL COMMENT '解析',
    difficulty  TINYINT      NOT NULL DEFAULT 3 COMMENT '难度1-5',
    use_count   INT          NOT NULL DEFAULT 0 COMMENT '被试卷引用次数',
    creator_id  BIGINT                DEFAULT NULL,
    status      TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_q_type_diff (type, difficulty),
    KEY idx_q_status (status)
) ENGINE = InnoDB COMMENT ='题目主表';

DROP TABLE IF EXISTS question_knowledge;
CREATE TABLE question_knowledge (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    question_id   BIGINT NOT NULL,
    knowledge_id  BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_qk_question (question_id),
    KEY idx_qk_knowledge (knowledge_id)
) ENGINE = InnoDB COMMENT ='题目-知识点';

-- ---------- 试卷 ----------
DROP TABLE IF EXISTS paper;
CREATE TABLE paper (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    name                VARCHAR(128) NOT NULL,
    total_score         INT          NOT NULL DEFAULT 0,
    suggest_duration    INT          NOT NULL DEFAULT 60 COMMENT '建议时长(分钟)',
    gen_type            VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    status              TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿1正式',
    constraint_snapshot TEXT                  DEFAULT NULL COMMENT '组卷约束快照',
    creator_id          BIGINT                DEFAULT NULL,
    create_time         DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT ='试卷';

DROP TABLE IF EXISTS paper_question;
CREATE TABLE paper_question (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    paper_id    BIGINT  NOT NULL,
    question_id BIGINT  NOT NULL,
    group_type  TINYINT NOT NULL,
    sort_no     INT     NOT NULL,
    score       INT     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_pq_paper (paper_id),
    KEY idx_pq_question (question_id)
) ENGINE = InnoDB COMMENT ='试卷题目';

-- ---------- 考试 ----------
DROP TABLE IF EXISTS exam;
CREATE TABLE exam (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    exam_name      VARCHAR(128) NOT NULL,
    paper_id       BIGINT       NOT NULL,
    class_id       BIGINT       NOT NULL,
    start_time     DATETIME     NOT NULL,
    end_time       DATETIME     NOT NULL,
    monitor_config TEXT                  DEFAULT NULL COMMENT '监考策略JSON',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '0未开始1进行中2已结束3已发布',
    creator_id     BIGINT                DEFAULT NULL,
    create_time    DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_exam_class_status (class_id, status),
    KEY idx_exam_end (end_time, status)
) ENGINE = InnoDB COMMENT ='考试';

DROP TABLE IF EXISTS exam_record;
CREATE TABLE exam_record (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    exam_id          BIGINT        NOT NULL,
    user_id          BIGINT        NOT NULL,
    status           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态机见枚举',
    start_time       DATETIME               DEFAULT NULL,
    submit_time      DATETIME               DEFAULT NULL,
    objective_score  DECIMAL(6,1)           DEFAULT 0,
    subjective_score DECIMAL(6,1)           DEFAULT 0,
    total_score      DECIMAL(6,1)           DEFAULT 0,
    violation_count  INT           NOT NULL DEFAULT 0,
    token_version    INT           NOT NULL DEFAULT 1,
    create_time      DATETIME               DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_exam_user (exam_id, user_id),
    KEY idx_er_exam_status (exam_id, status)
) ENGINE = InnoDB COMMENT ='考生考试记录';

DROP TABLE IF EXISTS exam_answer;
CREATE TABLE exam_answer (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    exam_id     BIGINT        NOT NULL,
    user_id     BIGINT        NOT NULL,
    question_id BIGINT        NOT NULL,
    content     TEXT                   DEFAULT NULL,
    score       DECIMAL(6,1)           DEFAULT NULL,
    comment     VARCHAR(512)           DEFAULT NULL,
    create_time DATETIME               DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_answer (exam_id, user_id, question_id)
) ENGINE = InnoDB COMMENT ='考生最终作答';

DROP TABLE IF EXISTS exam_violation;
CREATE TABLE exam_violation (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    exam_id     BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    type        VARCHAR(32)  NOT NULL,
    detail      VARCHAR(255)          DEFAULT NULL,
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ev_exam_user (exam_id, user_id)
) ENGINE = InnoDB COMMENT ='违规记录';

DROP TABLE IF EXISTS score_stat;
CREATE TABLE score_stat (
    id          BIGINT  NOT NULL AUTO_INCREMENT,
    exam_id     BIGINT  NOT NULL,
    class_id    BIGINT  NOT NULL,
    stat_json   TEXT    NOT NULL,
    version     INT     NOT NULL DEFAULT 1,
    create_time DATETIME         DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME         DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ss_exam (exam_id)
) ENGINE = InnoDB COMMENT ='统计快照';

-- ---------- 本地消息表（最终一致性） ----------
DROP TABLE IF EXISTS local_message;
CREATE TABLE local_message (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    msg_key         VARCHAR(64)  NOT NULL,
    biz_type        VARCHAR(16)  NOT NULL,
    exchange        VARCHAR(64)  NOT NULL,
    routing_key     VARCHAR(64)  NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    next_retry_time DATETIME              DEFAULT NULL,
    create_time     DATETIME              DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_msg_key (msg_key),
    KEY idx_lm_status_retry (status, next_retry_time)
) ENGINE = InnoDB COMMENT ='本地消息表';
