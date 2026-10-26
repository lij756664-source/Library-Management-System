-- 校园图书借阅自助管理系统 - 数据库脚本
-- MySQL 8.0+

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS book_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE book_db;

-- 删除表（按外键依赖顺序）
DROP TABLE IF EXISTS `borrow_record`;
DROP TABLE IF EXISTS `book`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `system_config`;
DROP TABLE IF EXISTS `user`;

-- 1. 用户表
CREATE TABLE `user` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN-管理员 USER-普通用户',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 图书分类表
CREATE TABLE `category` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- 3. 图书表
CREATE TABLE `book` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '图书ID',
    `title` VARCHAR(200) NOT NULL COMMENT '书名',
    `author` VARCHAR(100) DEFAULT NULL COMMENT '作者',
    `isbn` VARCHAR(20) DEFAULT NULL COMMENT 'ISBN编号',
    `category_id` INT NOT NULL COMMENT '分类ID',
    `price` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '价格',
    `stock` INT DEFAULT 0 COMMENT '可借库存',
    `description` TEXT COMMENT '简介',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    CONSTRAINT `fk_book_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书表';

-- 4. 借阅记录表
CREATE TABLE `borrow_record` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '借阅记录ID',
    `user_id` INT NOT NULL COMMENT '借阅用户ID',
    `book_id` INT NOT NULL COMMENT '图书ID',
    `borrow_time` DATETIME NOT NULL COMMENT '借阅时间',
    `due_time` DATETIME NOT NULL COMMENT '应还时间',
    `return_time` DATETIME DEFAULT NULL COMMENT '实际归还时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'BORROWING' COMMENT '状态：BORROWING-借阅中 RETURNED-已归还',
    `renew_count` INT NOT NULL DEFAULT 0 COMMENT '已续借次数',
    `overdue_days` INT DEFAULT 0 COMMENT '逾期天数',
    `fine_amount` DECIMAL(10, 2) DEFAULT 0.00 COMMENT '罚款金额',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_book_id` (`book_id`),
    KEY `idx_status` (`status`),
    KEY `idx_due_time` (`due_time`),
    CONSTRAINT `fk_borrow_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_borrow_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录表';

-- 5. 系统配置表
CREATE TABLE `system_config` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(100) NOT NULL COMMENT '配置值',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '配置说明',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 初始用户（密码明文，仅演示）
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '123456', '管理员', 'ADMIN'),
('user', '123456', '普通用户', 'USER');

-- 初始分类
INSERT INTO `category` (`name`, `description`) VALUES
('计算机', '计算机与编程类图书'),
('教材', '高校教材类图书'),
('科幻', '科幻小说'),
('文学', '文学类图书'),
('历史', '历史类图书');

-- 初始图书
INSERT INTO `book` (`title`, `author`, `isbn`, `category_id`, `price`, `stock`, `description`) VALUES
('Java编程思想', 'Bruce Eckel', '978-7-111-21382-6', 1, 108.00, 5, 'Java经典教材，深入浅出讲解面向对象编程'),
('Spring Boot实战', 'Craig Walls', '978-7-115-41917-5', 1, 79.00, 3, 'Spring Boot快速入门与实战指南'),
('Vue.js设计与实现', '霍春阳', '978-7-115-59176-4', 1, 99.00, 4, '深入理解Vue 3框架原理'),
('数据库系统概论', '王珊', '978-7-04-040664-1', 2, 45.00, 10, '数据库经典教材'),
('三体', '刘慈欣', '978-7-5366-9293-0', 3, 68.00, 8, '中国科幻文学里程碑之作'),
('活着', '余华', '978-7-5063-4910-9', 4, 35.00, 6, '讲述一个人一生的故事'),
('人类简史', '尤瓦尔·赫拉利', '978-7-5086-4482-4', 5, 68.00, 4, '从动物到上帝的人类发展史'),
('深入理解计算机系统', 'Randal E. Bryant', '978-7-111-54493-7', 1, 139.00, 2, '计算机系统经典教材');

-- 系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('borrow_days', '30', '默认借阅天数'),
('renew_days', '14', '每次续借延长天数'),
('max_renew_count', '2', '最大续借次数'),
('fine_per_day', '0.5', '逾期每天罚款（元）'),
('max_borrow_count', '5', '每人最多同时借阅册数');

-- 示例借阅记录（便于演示统计）
INSERT INTO `borrow_record` (`user_id`, `book_id`, `borrow_time`, `due_time`, `status`, `renew_count`) VALUES
(2, 5, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), 'BORROWING', 0),
(2, 6, DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'BORROWING', 1);

-- 同步扣减示例借阅的库存
UPDATE `book` SET `stock` = `stock` - 1 WHERE `id` IN (5, 6);
