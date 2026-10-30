-- =============================================================
-- 校园图书借阅与资源管理系统 - 用户与权限子系统数据库脚本
-- MySQL 8.0+
-- 执行顺序：先执行 database.sql（已初始化过的库直接跳过），再执行本脚本
-- 说明：
--   1. 第 1 节 ALTER 仅需执行一次，重复执行报“列已存在”属正常现象
--   2. 第 2~7 节可重复执行（幂等）
-- =============================================================

SET NAMES utf8mb4;

USE book_db;

-- ---------------------------------------------------------------
-- 0. 清理本子系统新增表（便于重复初始化）
-- ---------------------------------------------------------------
DROP TABLE IF EXISTS `operation_log`;
DROP TABLE IF EXISTS `user_role`;
DROP TABLE IF EXISTS `role`;

-- ---------------------------------------------------------------
-- 1. 用户表升级（在 database.sql 的 user 表基础上补充字段）
-- ---------------------------------------------------------------
ALTER TABLE `user`
    ADD COLUMN `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER `nickname`,
    ADD COLUMN `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER `email`,
    ADD COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '账号状态：1-启用 0-停用' AFTER `role`,
    ADD COLUMN `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `create_time`;

-- ---------------------------------------------------------------
-- 2. 角色表
-- ---------------------------------------------------------------
CREATE TABLE `role` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `code` VARCHAR(20) NOT NULL COMMENT '角色编码：ADMIN-图书管理员 USER-普通读者',
    `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ---------------------------------------------------------------
-- 3. 用户-角色关联表
--    user.role 为冗余字段（供现有查询/统计模块直接使用），本表为规范化
--    关联记录，注册时两处同步写入
-- ---------------------------------------------------------------
CREATE TABLE `user_role` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` INT NOT NULL COMMENT '用户ID',
    `role_id` INT NOT NULL COMMENT '角色ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_role_role` (`role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色关联表';

-- ---------------------------------------------------------------
-- 4. 操作日志表（所有账号相关操作均写入，便于追溯）
-- ---------------------------------------------------------------
CREATE TABLE `operation_log` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `user_id` INT DEFAULT NULL COMMENT '操作人用户ID（未登录操作可为空）',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
    `operation` VARCHAR(50) NOT NULL COMMENT '操作类型：注册/登录/修改个人信息/修改密码/启用账号/停用账号/重置密码等',
    `detail` VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
    `ip` VARCHAR(50) DEFAULT NULL COMMENT '操作来源IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_log_user_id` (`user_id`),
    KEY `idx_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ---------------------------------------------------------------
-- 5. 初始化角色数据（INSERT IGNORE 保证重复执行安全）
-- ---------------------------------------------------------------
INSERT IGNORE INTO `role` (`code`, `name`, `description`) VALUES
('ADMIN', '图书管理员', '拥有读者账号管理与系统管理权限'),
('USER', '普通读者', '可登录系统、借阅图书、维护个人信息');

-- ---------------------------------------------------------------
-- 6. 为存量用户补齐 user_role 关联记录（重复执行安全）
-- ---------------------------------------------------------------
INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id
FROM `user` u
JOIN `role` r ON u.role = r.code
WHERE NOT EXISTS (
    SELECT 1 FROM `user_role` ur WHERE ur.user_id = u.id AND ur.role_id = r.id
);

-- ---------------------------------------------------------------
-- 7. 存量明文密码升级为 BCrypt 加密存储（升级后密码仍为 123456）
--    全新执行 database.sql 的库已直接插入 BCrypt 密文，此语句不会命中
-- ---------------------------------------------------------------
UPDATE `user`
SET `password` = '$2b$10$sAO0f0onq4ER9jNvxPl15ejsgry7mvMlscmxpdMGRTWPr59w3sq0u'
WHERE `password` = '123456';
