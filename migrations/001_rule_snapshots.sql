-- 升级前备份数据库并停止应用；仅执行一次。MySQL DDL 隐式提交，失败时检查结构后处理，不要盲目重跑。
-- 先补旧记录，再重置当前配置。已归还金额不变，预约配置不变。
ALTER TABLE borrow_record
 ADD borrow_days_snapshot INT NULL,
 ADD max_renew_count_snapshot INT NULL,
 ADD renew_days_snapshot INT NULL,
 ADD fine_per_day_snapshot DECIMAL(3,2) NULL,
 MODIFY fine_amount DECIMAL(12,2) DEFAULT 0.00;
START TRANSACTION;
UPDATE borrow_record SET
 borrow_days_snapshot=COALESCE((SELECT CAST(config_value AS UNSIGNED) FROM system_config WHERE config_key='borrow_days'),30),
 max_renew_count_snapshot=COALESCE((SELECT CAST(config_value AS UNSIGNED) FROM system_config WHERE config_key='max_renew_count'),2),
 renew_days_snapshot=COALESCE((SELECT CAST(config_value AS UNSIGNED) FROM system_config WHERE config_key='renew_days'),14),
 fine_per_day_snapshot=COALESCE((SELECT CAST(config_value AS DECIMAL(3,2)) FROM system_config WHERE config_key='fine_per_day'),0.50);
INSERT INTO system_config(config_key,config_value,description) VALUES
 ('borrow_days','30','借阅期限'),('max_renew_count','2','续借上限'),('renew_days','7','每次续借天数'),('fine_per_day','0.50','每日罚金'),('max_borrow_count','5','同时借阅上限')
 ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);
COMMIT;
ALTER TABLE borrow_record
 MODIFY borrow_days_snapshot INT NOT NULL,
 MODIFY max_renew_count_snapshot INT NOT NULL,
 MODIFY renew_days_snapshot INT NOT NULL,
 MODIFY fine_per_day_snapshot DECIMAL(3,2) NOT NULL;
CREATE INDEX idx_borrow_time_book ON borrow_record(borrow_time,book_id);
