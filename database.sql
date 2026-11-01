-- 校园图书借阅自助管理系统 - 数据库脚本
-- MySQL 8.0+

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS book_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE book_db;

-- 删除表（按外键依赖顺序）
DROP TABLE IF EXISTS `reservation`;
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
    `location` VARCHAR(100) DEFAULT NULL COMMENT '馆藏位置（馆室/书架），读者可查询的公开馆藏信息',
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

-- 5. 预约排队表
CREATE TABLE `reservation` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    `user_id` INT NOT NULL COMMENT '预约用户ID',
    `book_id` INT NOT NULL COMMENT '图书ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING-排队中 READY-待取书 FULFILLED-已取书 CANCELLED-已取消 EXPIRED-已过期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预约时间',
    `expire_time` DATETIME DEFAULT NULL COMMENT '待取书截止时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_res_user_id` (`user_id`),
    KEY `idx_res_book_id` (`book_id`),
    KEY `idx_res_status` (`status`),
    CONSTRAINT `fk_res_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_res_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书预约表';

-- 6. 系统配置表
CREATE TABLE `system_config` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key` VARCHAR(50) NOT NULL COMMENT '配置键',
    `config_value` VARCHAR(100) NOT NULL COMMENT '配置值',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '配置说明',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 初始用户
INSERT INTO `user` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2b$10$sAO0f0onq4ER9jNvxPl15ejsgry7mvMlscmxpdMGRTWPr59w3sq0u', '管理员', 'ADMIN'),
('user', '$2b$10$sAO0f0onq4ER9jNvxPl15ejsgry7mvMlscmxpdMGRTWPr59w3sq0u', '读者甲', 'USER'),
('user2', '$2b$10$sAO0f0onq4ER9jNvxPl15ejsgry7mvMlscmxpdMGRTWPr59w3sq0u', '读者乙', 'USER');

-- 初始分类
INSERT INTO `category` (`name`, `description`) VALUES
('计算机', '计算机与编程类图书'),
('教材', '高校教材类图书'),
('科幻', '科幻小说'),
('文学', '文学类图书'),
('历史', '历史类图书');

-- 初始图书
INSERT INTO `book` (`title`, `author`, `isbn`, `category_id`, `price`, `stock`, `location`, `description`) VALUES
                                                                                                               ('Java编程思想', 'Bruce Eckel', '978-7-111-21382-6', 1, 108.00, 5, '三楼计算机阅览室 A-01 书架', 'Java经典教材，深入浅出讲解面向对象编程'),
                                                                                                               ('Spring Boot实战', 'Craig Walls', '978-7-115-41917-5', 1, 79.00, 3, '三楼计算机阅览室 A-02 书架', 'Spring Boot快速入门与实战指南'),
                                                                                                               ('Vue.js设计与实现', '霍春阳', '978-7-115-59176-4', 1, 99.00, 4, '三楼计算机阅览室 A-02 书架', '深入理解Vue 3框架原理'),
                                                                                                               ('数据库系统概论', '王珊', '978-7-04-040664-1', 2, 45.00, 10, '四楼教材库 B-05 书架', '数据库经典教材'),
                                                                                                               ('三体', '刘慈欣', '978-7-5366-9293-0', 3, 68.00, 8, '二楼文学阅览室 C-03 书架', '中国科幻文学里程碑之作'),
                                                                                                               ('活着', '余华', '978-7-5063-4910-9', 4, 35.00, 6, '二楼文学阅览室 C-01 书架', '讲述一个人一生的故事'),
                                                                                                               ('人类简史', '尤瓦尔·赫拉利', '978-7-5086-4482-4', 5, 68.00, 4, '五楼社科阅览室 D-02 书架', '从动物到上帝的人类发展史'),
                                                                                                               ('深入理解计算机系统', 'Randal E. Bryant', '978-7-111-54493-7', 1, 139.00, 2, '三楼计算机阅览室 A-01 书架', '计算机系统经典教材'),
                                                                                                               ('代码整洁之道', 'Robert C. Martin', '978-7-115-21687-8', 1, 59.00, 0, '三楼计算机阅览室 A-03 书架', '软件工程领域的经典之作，教你如何编写可读、可维护的代码'),
                                                                                                               ('计算机网络：自顶向下方法', 'James F. Kurose', '978-7-111-59992-4', 2, 89.00, 0, '四楼教材库 B-02 书架', '计算机网络的经典教材，强调原理与实践的结合'),
                                                                                                               ('数据结构与算法分析', 'Mark Allen Weiss', '978-7-111-52720-9', 2, 65.00, 0, '四楼教材库 B-06 书架', '详细讲解了数据结构与算法分析的核心概念'),
                                                                                                               ('银河系漫游指南', '道格拉斯·亚当斯', '978-7-5366-9294-7', 3, 42.00, 0, '二楼文学阅览室 C-04 书架', '充满英式幽默的科幻经典，关于生命、宇宙以及一切的终极答案'),
                                                                                                               ('沙丘', '弗兰克·赫伯特', '978-7-5399-6648-3', 3, 69.00, 0, '二楼文学阅览室 C-05 书架', '科幻史上的必读经典，构建了宏大的生态与政治体系'),
                                                                                                               ('百年孤独', '加西亚·马尔克斯', '978-7-5442-5399-4', 4, 55.00, 0, '二楼文学阅览室 C-02 书架', '魔幻现实主义文学的代表作，布恩迪亚家族七代人的传奇故事'),
                                                                                                               ('万历十五年', '黄仁宇', '978-7-108-00982-1', 5, 28.00, 0, '五楼社科阅览室 D-01 书架', '从大历史观的角度剖析明代社会的症结'),
                                                                                                               ('明朝那些事儿', '当年明月', '978-7-80244-748-2', 5, 358.00, 0, '五楼社科阅览室 D-03 书架', '以幽默风趣的笔法讲述明朝三百年历史'),
                                                                                                               ('重构：改善既有代码的设计', 'Martin Fowler', '978-7-115-50848-5', 1, 99.00, 4, '三楼计算机阅览室 A-05 书架', '软件工程领域的经典，教你如何安全地重构代码'),
                                                                                                               ('操作系统概念', 'Abraham Silberschatz', '978-7-111-52377-1', 2, 99.00, 6, '四楼教材库 B-07 书架', '操作系统领域的经典教材，全面讲解核心概念'),
                                                                                                               ('海伯利安', '丹·西蒙斯', '978-7-5534-9294-9', 3, 78.00, 3, '二楼文学阅览室 C-06 书架', '科幻史诗巨作，融合了宇宙、宗教与人类的命运'),
                                                                                                               ('白夜行', '东野圭吾', '978-7-5442-5427-4', 4, 39.50, 5, '二楼文学阅览室 C-07 书架', '日本推理小说的巅峰之作，无望却坚守的凄凉爱情'),
                                                                                                               ('枪炮、病菌与钢铁', '贾雷德·戴蒙德', '978-7-5446-4198-1', 5, 68.00, 4, '五楼社科阅览室 D-04 书架', '人类大历史经典，探讨人类社会不平等的根源'),
                                                                                                               ('深入理解Java虚拟机', '周志明', '978-7-111-64124-7', 1, 129.00, 2, '三楼计算机阅览室 A-06 书架', 'Java开发者必读，全面剖析JVM内存模型与垃圾回收');
-- 系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `description`) VALUES
('borrow_days', '30', '默认借阅天数'),
('renew_days', '14', '每次续借延长天数'),
('max_renew_count', '2', '最大续借次数'),
('fine_per_day', '0.5', '逾期每天罚款（元）'),
('max_borrow_count', '5', '每人最多同时借阅册数'),
('max_reservation_count', '3', '每人最多同时预约本数'),
('pickup_days', '3', '预约到书后取书期限（天）');

-- 示例借阅记录（便于演示统计）
INSERT INTO `borrow_record` (`user_id`, `book_id`, `borrow_time`, `due_time`, `status`, `renew_count`) VALUES
(2, 5, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 20 DAY), 'BORROWING', 0),
(2, 6, DATE_SUB(NOW(), INTERVAL 35 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'BORROWING', 1);

-- 同步扣减示例借阅的库存
UPDATE `book` SET `stock` = `stock` - 1 WHERE `id` IN (5, 6);
ALTER TABLE `user` ADD COLUMN email VARCHAR(255) NULL, ADD COLUMN phone VARCHAR(255) NULL;
