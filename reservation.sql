-- 为已有数据库增量添加预约表与配置（不影响现有数据）
SET NAMES utf8mb4;
USE book_db;

CREATE TABLE IF NOT EXISTS `reservation` (
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

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
SELECT 'max_reservation_count', '3', '每人最多同时预约本数'
WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'max_reservation_count');

INSERT INTO `system_config` (`config_key`, `config_value`, `description`)
SELECT 'pickup_days', '3', '预约到书后取书期限（天）'
WHERE NOT EXISTS (SELECT 1 FROM `system_config` WHERE `config_key` = 'pickup_days');

INSERT INTO `user` (`username`, `password`, `nickname`, `role`)
SELECT 'user2', '123456', '读者乙', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'user2');
