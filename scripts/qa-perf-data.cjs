const fs=require('fs'),{execFileSync}=require('child_process');
const { credential } = require('./qa-config.cjs');
const env={...process.env,MYSQL_PWD:credential('password')};const cli='C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';const args=['--default-character-set=utf8mb4','-h','127.0.0.1','-u',credential('username'),'-N','-B'];
const sql=q=>execFileSync(cli,[...args,'-e',q],{env,encoding:'utf8'}).trim();
if(sql("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='book_db_perf';")!=='0')throw Error('性能库已存在，停止，避免覆盖');
const base=fs.readFileSync('database.sql','utf8'),permission=fs.readFileSync('user_permission.sql','utf8'),avatar=fs.readFileSync('avatar.sql','utf8');
const create=s=>[...s.matchAll(/CREATE TABLE `[^`]+` \([\s\S]*?\) ENGINE[^;]+;/g)].map(m=>m[0]).join('\n');
let out='CREATE DATABASE book_db_perf CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; USE book_db_perf;\n'+create(base)+'\n'+permission.match(/ALTER TABLE `user`[\s\S]*?;/)[0]+'\n'+create(permission)+'\n'+avatar.match(/ALTER TABLE `user`[\s\S]*?;/)[0]+'\nALTER TABLE borrow_record MODIFY fine_amount DECIMAL(12,2) DEFAULT 0.00; CREATE INDEX idx_borrow_time_book ON borrow_record(borrow_time,book_id);\n';
out+="INSERT INTO role(id,code,name) VALUES(1,'ADMIN','管理员'),(2,'USER','读者');\nINSERT INTO user(id,username,password,nickname,role,status) SELECT 1,'admin',password,'管理员','ADMIN',1 FROM book_db_current.user WHERE username='admin';\n";
for(let i=1;i<=500;i++)out+=`INSERT INTO user(id,username,password,nickname,role,status) SELECT ${i+1},'perf${i}',password,'性能读者${i}','USER',1 FROM book_db_current.user WHERE username='admin';\n`;
out+="INSERT INTO user_role(user_id,role_id) SELECT id,IF(role='ADMIN',1,2) FROM user;\nINSERT INTO system_config(config_key,config_value) VALUES('borrow_days','30'),('max_renew_count','2'),('renew_days','7'),('fine_per_day','0.50'),('max_borrow_count','5'),('max_reservation_count','3'),('pickup_days','3');\n";
for(let i=1;i<=10;i++)out+=`INSERT INTO category(id,name) VALUES(${i},'性能类别${i}');\n`;
for(let i=1;i<=1000;i++)out+=`INSERT INTO book(id,title,author,category_id,stock) VALUES(${i},'性能图书${i}','测试作者',${(i-1)%10+1},2);\n`;
const rows=[];const day=(date,n=0)=>new Date(Date.parse(date+'T00:00:00Z')+n*86400000).toISOString().slice(0,10);
for(let i=0;i<9000;i++){
 const user=i%500+2,book=((i*37+Math.floor(i/500)*13)%1000)+1,date=day('2024-01-01',Math.floor(i/500)*40+(i%500)%7);const renew=i%7===0?1:0,due=day(date,30+renew*7),late=!renew&&i%10===0,returned=day(date,late?32:5);
 rows.push({user,book,date,due,returned,renew,overdue:late?2:0,fine:late?'1.00':'0.00'});
}
for(let i=0;i<1000;i++){
 const date=day('2026-11-01',i<200?-35:-10);rows.push({user:i%500+2,book:i+1,date,due:day(date,30),returned:null,renew:0,overdue:0,fine:'0.00'});
}
for(let i=0;i<rows.length;i+=500){out+='INSERT INTO borrow_record(user_id,book_id,borrow_time,due_time,return_time,status,renew_count,borrow_days_snapshot,max_renew_count_snapshot,renew_days_snapshot,fine_per_day_snapshot,overdue_days,fine_amount) VALUES\n';out+=rows.slice(i,i+500).map(r=>`(${r.user},${r.book},'${r.date} 09:00:00','${r.due} 09:00:00',${r.returned?"'"+r.returned+" 10:00:00'":'NULL'},'${r.returned?'RETURNED':'BORROWING'}',${r.renew},30,2,7,0.50,${r.overdue},${r.fine})`).join(',\n')+';\n';}
execFileSync(cli,args,{env,input:out,encoding:'utf8',maxBuffer:4*1024*1024});
const ranges=[{name:'全部历史',start:'',end:''},{name:'跨12个月',start:'2024-01-01',end:'2024-12-31'},{name:'单月',start:'2024-02-01',end:'2024-02-29'},{name:'单日',start:'2024-01-01',end:'2024-01-01'},{name:'无数据',start:'2000-01-01',end:'2000-01-01'}];
for(const range of ranges){const selected=rows.filter(r=>!range.start||(r.date>=range.start&&r.date<=range.end));let counts=new Map(),categories=Array(10).fill(0);for(const r of selected){counts.set(r.book,(counts.get(r.book)||0)+1);categories[(r.book-1)%10]++}range.expected={total:selected.length,top:[...counts].sort((a,b)=>b[1]-a[1]||a[0]-b[0]).slice(0,10).map(([id,count])=>({bookTitle:'性能图书'+id,borrowCount:count})),categories:categories.map((count,i)=>({categoryName:'性能类别'+(i+1),borrowCount:count})).sort((a,b)=>b.borrowCount-a.borrowCount||Number(a.categoryName.replace('性能类别',''))-Number(b.categoryName.replace('性能类别','')))};}
fs.writeFileSync('docs/性能测试预期.json',JSON.stringify({createdAt:new Date().toISOString(),scale:{books:1000,totalCopies:3000,available:2000,categories:10,readers:500,borrows:10000,active:1000,overdueActive:200},ranges},null,2));
console.log(sql("SELECT (SELECT COUNT(*) FROM book_db_perf.book) books,(SELECT COUNT(*) FROM book_db_perf.borrow_record) borrows,(SELECT COUNT(*) FROM book_db_perf.borrow_record WHERE status='BORROWING') active,(SELECT COUNT(*) FROM book_db_perf.borrow_record WHERE status='BORROWING' AND DATE(due_time)<'2026-11-01') overdue;"));
