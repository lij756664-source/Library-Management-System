// 独立测试账号的并发验收脚本；不修改应用实现。锁等待证据写入脱敏结果文件。
const fs = require('fs');
const { spawn, execFileSync } = require('child_process');
const mysql = 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';
const { credential } = require('./qa-config.cjs');
const env = { ...process.env, MYSQL_PWD: credential('password') };
const args = ['--default-character-set=utf8mb4','-h','127.0.0.1','-u',credential('username'),'-N','-B'];
const sql = query => execFileSync(mysql,[...args,'-e',query],{env,encoding:'utf8'}).trim();
const delay = ms => new Promise(resolve=>setTimeout(resolve,ms));
const api = 'http://127.0.0.1:8080/api';
const results = [];
async function call(path, token, method='GET', body) {
 const response=await fetch(api+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body===undefined?{}:{body:JSON.stringify(body)})});
 return response.json();
}
function assert(ok,message){if(!ok)throw Error(message)}
async function success(path,token,method,body){const response=await call(path,token,method,body);assert(response.code===200,path+': '+response.message);return response.data}
async function overlap(name, actions) {
 const holder=spawn(mysql,[...args,'--unbuffered'],{env,stdio:['pipe','pipe','pipe']});
 let output='',errors='';holder.stdout.on('data',chunk=>output+=chunk);holder.stderr.on('data',chunk=>errors+=chunk);
 const closed=new Promise(resolve=>holder.on('close',resolve));
 holder.stdin.write("USE book_db_current; START TRANSACTION; SELECT id FROM system_config WHERE config_key IN ('borrow_days','max_renew_count','renew_days','fine_per_day','max_borrow_count') ORDER BY config_key FOR UPDATE; SELECT 'QA_LOCK_READY';\n");
 let pending=[],waiting=0;
 try {
  for(let i=0;i<50&&!output.includes('QA_LOCK_READY');i++)await delay(100);
  assert(output.includes('QA_LOCK_READY'),'未取得测试屏障锁 '+errors);
  const startedAt=new Date().toISOString();
  pending=actions.map(action=>action().then(response=>({code:response.code,message:response.message,completedAt:new Date().toISOString()})));
  for(let i=0;i<40;i++) {
   waiting=Number(sql("SELECT COUNT(*) FROM information_schema.INNODB_TRX WHERE trx_state='LOCK WAIT' AND trx_query LIKE '%system_config%';"));
   if(waiting>=2)break;
   await delay(100);
  }
  holder.stdin.end('COMMIT;\nquit\n');await closed;
  const responses=await Promise.all(pending);
  assert(waiting>=2,'未证明两个请求在服务端重叠，不计为并发通过');
  const result={name,startedAt,observedLockWaiters:waiting,responses};results.push(result);return result;
 } finally {
  if(holder.exitCode===null){holder.stdin.end('ROLLBACK;\nquit\n');await closed}
  await Promise.allSettled(pending);
 }
}
(async()=>{let admin,owner,receiver,original,book;try{
admin=(await success('/auth/login',null,'POST',{username:'admin',password:'123456'})).token;
owner=(await success('/auth/login',null,'POST',{username:'qa_parallel',password:'123456'})).token;
receiver=(await success('/auth/login',null,'POST',{username:'qa_date',password:'123456'})).token;
original=await success('/rules',admin);
const ruleA={borrow_days:'3',max_renew_count:'2',renew_days:'7',fine_per_day:'0.50',max_borrow_count:'3'};
const ruleB={borrow_days:'10',max_renew_count:'1',renew_days:'3',fine_per_day:'1.00',max_borrow_count:'3'};
await success('/rules',admin,'PUT',ruleA);
const categories=await success('/categories',admin),title='QA取书并发'+Date.now();
await success('/books',admin,'POST',{title,author:'测试作者',categoryId:categories[0].id,stock:1,price:0,description:'规则与预约取书重叠测试'});
book=(await success('/books?keyword='+encodeURIComponent(title),admin)).find(b=>b.title===title);
await success('/borrows',owner,'POST',{bookId:book.id});await success('/reservations',receiver,'POST',{bookId:book.id});
const loan=(await success('/borrows/my',owner)).find(r=>r.bookId===book.id&&r.status==='BORROWING');await success('/borrows/'+loan.id+'/return',owner,'PUT');
const reservation=(await success('/reservations/my',receiver)).find(r=>r.bookId===book.id);assert(reservation.status==='READY','预约未就绪');
const result=await overlap('H04补充：改规则与预约取书',[()=>call('/rules',admin,'PUT',ruleB),()=>call('/reservations/'+reservation.id+'/pickup',receiver,'PUT')]);
const row=(await success('/borrows/my',receiver)).find(r=>r.bookId===book.id&&r.status==='BORROWING');const snapshot=[row.borrowDaysSnapshot,row.maxRenewCountSnapshot,row.renewDaysSnapshot,Number(row.finePerDaySnapshot)];
assert(result.responses.every(r=>r.code===200)&&['[3,2,7,0.5]','[10,1,3,1]'].includes(JSON.stringify(snapshot)),'取书规则混组');
assert((Date.parse(row.dueTime.slice(0,10))-Date.parse(row.borrowTime.slice(0,10)))/86400000===snapshot[0],'应还日期错误');
result.passed=true;result.snapshot=snapshot;
}catch(error){results.push({error:error.message});process.exitCode=1}
finally{try{if(book)for(const token of [owner,receiver])if(token)for(const row of(await success('/borrows/my',token)).filter(r=>r.bookId===book.id&&r.status==='BORROWING'))await success('/borrows/'+row.id+'/return',token,'PUT');if(admin&&original)await success('/rules',admin,'PUT',original)}catch(error){results.push({cleanupError:error.message});process.exitCode=1}fs.writeFileSync('docs/预约取书并发结果.json',JSON.stringify(results,null,2));console.log(JSON.stringify(results,null,2))}})();