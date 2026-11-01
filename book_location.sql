-- =============================================================
-- 图书资源管理模块 - 增量数据库脚本（仅涉及图书模块自身的 book 表）
-- MySQL 8.0+
-- 说明：本脚本独立于 database.sql，可在已有数据库上安全重复执行（幂等），
--       新增 book.location（馆藏位置）字段，供管理员维护、读者查询公开馆藏位置。
-- =============================================================

SET NAMES utf8mb4;
USE book_db;

-- ---------------------------------------------------------------
-- 1. 安全升级 book 表：补充 location（馆藏位置）字段
-- （先检查字段是否存在，不存在才添加，避免报错中断脚本）
-- ---------------------------------------------------------------
SET @dbname = DATABASE();
SET @tablename = 'book';
SET @columnname = 'location';
SET @preparedStatement = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname
       AND TABLE_NAME = @tablename
       AND COLUMN_NAME = @columnname) > 0,
    'SELECT 1',
    'ALTER TABLE `book` ADD COLUMN `location` VARCHAR(100) DEFAULT NULL COMMENT ''馆藏位置（馆室/书架），读者可查询的公开馆藏信息'' AFTER `stock`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- ---------------------------------------------------------------
-- 2. 为示例馆藏补充馆藏位置（仅填充为空的数据，不覆盖管理员已维护的内容）
-- ---------------------------------------------------------------
UPDATE `book` SET `location` = '三楼计算机阅览室 A-01 书架'
WHERE `isbn` IN ('978-7-111-21382-6', '978-7-111-54493-7') AND (`location` IS NULL OR `location` = '');

UPDATE `book` SET `location` = '三楼计算机阅览室 A-02 书架'
WHERE `isbn` IN ('978-7-115-41917-5', '978-7-115-59176-4') AND (`location` IS NULL OR `location` = '');

UPDATE `book` SET `location` = '四楼教材库 B-05 书架'
WHERE `isbn` = '978-7-04-040664-1' AND (`location` IS NULL OR `location` = '');

UPDATE `book` SET `location` = '二楼文学阅览室 C-03 书架'
WHERE `isbn` = '978-7-5366-9293-0' AND (`location` IS NULL OR `location` = '');

UPDATE `book` SET `location` = '二楼文学阅览室 C-01 书架'
WHERE `isbn` = '978-7-5063-4910-9' AND (`location` IS NULL OR `location` = '');

UPDATE `book` SET `location` = '五楼社科阅览室 D-02 书架'
WHERE `isbn` = '978-7-5086-4482-4' AND (`location` IS NULL OR `location` = '');
