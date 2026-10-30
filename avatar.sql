-- =============================================================
-- 校园图书借阅与资源管理系统 - 头像功能数据库脚本
-- MySQL 8.0+
-- 说明：在 user_permission.sql 基础上为 user 表补充头像字段。
--       重复执行报“列已存在”属正常现象。
-- =============================================================

SET NAMES utf8mb4;

USE book_db;

-- 用户表增加头像字段（保存 /uploads/avatars/ 下的相对地址）
ALTER TABLE `user`
    ADD COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像地址（上传后为 /uploads/avatars/xxx 形式）' AFTER `phone`;
