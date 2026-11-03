const fs=require('fs'),{execFileSync}=require('child_process'),{credential}=require('./qa-config.cjs');
const mysql='C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe',env={...process.env,MYSQL_PWD:credential('password')};
const run=sql=>execFileSync(mysql,['--default-character-set=utf8mb4','-h','127.0.0.1','-u',credential('username'),'-N','-B'],{env,input:sql,encoding:'utf8'}).trim();
const fresh='book_db_integration_20261102c',old='book_db_migration_20261102';
if(run("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='"+fresh+"'")!=='0') throw Error('Refusing overwrite');
for(const file of ['database.sql','user_permission.sql','avatar.sql'])run(fs.readFileSync(file,'utf8').replace(/book_db/g,fresh));
// Migration fixture only needs the independently idempotent catalogue patch.
run(fs.readFileSync('book_location.sql','utf8').replace(/book_db/g,old));
run(fs.readFileSync('book_location.sql','utf8').replace(/book_db/g,old));
const checks={};for(const db of [fresh,old])checks[db+'Location']=run("SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='"+db+"' AND TABLE_NAME='book' AND COLUMN_NAME='location'")==='1';
checks.freshFinePrecision=run("SELECT NUMERIC_PRECISION,NUMERIC_SCALE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='"+fresh+"' AND TABLE_NAME='borrow_record' AND COLUMN_NAME='fine_amount'")==='12\t2';
checks.oldFinalFinePreserved=run('SELECT fine_amount FROM '+old+'.borrow_record WHERE id=2')==='6.25';
checks.oldSnapshotsPreserved=run('SELECT COUNT(*) FROM '+old+'.borrow_record WHERE borrow_days_snapshot=45 AND max_renew_count_snapshot=3 AND renew_days_snapshot=14 AND fine_per_day_snapshot=1.25')==='2';
fs.writeFileSync('docs/整合数据库验证.json',JSON.stringify({testedAt:new Date().toISOString(),freshDatabase:fresh,migratedDatabase:old,checks,passed:Object.values(checks).every(Boolean)},null,2));console.log(checks);if(!Object.values(checks).every(Boolean))process.exitCode=1;
