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
(async()=>{
 let admin,reader,original;
 try {
  admin=(await success('/auth/login',null,'POST',{username:'admin',password:'123456'})).token;
  original=await success('/rules',admin);
  const username='qa_parallel';
  const existing=sql("SELECT COUNT(*) FROM book_db_current.user WHERE username='qa_parallel';");
  if(existing==='0')await success('/auth/register',null,'POST',{username,password:'123456',nickname:username});
  reader=(await success('/auth/login',null,'POST',{username,password:'123456'})).token;
  let rows=await success('/borrows/my',reader);assert(rows.every(row=>row.status!=='BORROWING'),'独立测试账号有未还记录，停止');
  const ruleA={borrow_days:'3',max_renew_count:'2',renew_days:'7',fine_per_day:'0.50',max_borrow_count:'3'};
  const ruleB={borrow_days:'10',max_renew_count:'1',renew_days:'3',fine_per_day:'1.00',max_borrow_count:'3'};
  await success('/rules',admin,'PUT',ruleA);
  const categories=await success('/categories',admin);const categoryId=categories[0].id;
  const books=[];
  const suffix=Date.now();
  for(let i=0;i<5;i++) {
   const title='QA并发'+suffix+'-'+i;
   await success('/books',admin,'POST',{title,author:'测试作者',categoryId,stock:1,price:0,description:'同时操作测试专用'});
   const matches=await success('/books?keyword='+encodeURIComponent(title),admin);books.push(matches.find(book=>book.title===title));
  }
  await success('/borrows',reader,'POST',{bookId:books[0].id});await success('/borrows',reader,'POST',{bookId:books[1].id});
  const h1=await overlap('H01最后一个借阅名额',[()=>call('/borrows',reader,'POST',{bookId:books[2].id}),()=>call('/borrows',reader,'POST',{bookId:books[3].id})]);
  rows=await success('/borrows/my',reader);let active=rows.filter(row=>row.status==='BORROWING');
  assert(h1.responses.filter(r=>r.code===200).length===1&&active.length===3,'H01超限或结果错误');
  for(const book of books.slice(2,4)){const detail=await success('/books/'+book.id,admin);const borrowed=active.some(row=>row.bookId===book.id);assert(detail.stock===(borrowed?0:1),'H01库存错误')}
  h1.passed=true;h1.activeCount=active.length;
  const target=active.find(row=>[books[2].id,books[3].id].includes(row.bookId));
  await success('/borrows/'+target.id+'/renew',reader,'PUT');
  const before=(await success('/borrows/my',reader)).find(row=>row.id===target.id);
  const h2=await overlap('H02最后一次续借',[()=>call('/borrows/'+target.id+'/renew',reader,'PUT'),()=>call('/borrows/'+target.id+'/renew',reader,'PUT')]);
  const renewed=(await success('/borrows/my',reader)).find(row=>row.id===target.id);
  assert(h2.responses.filter(r=>r.code===200).length===1&&renewed.renewCount===2,'H02次数错误');
  assert(new Date(renewed.dueTime)-new Date(before.dueTime)===7*86400000,'H02日期延长不止一次');h2.passed=true;
  const h3=await overlap('H03重复归还',[()=>call('/borrows/'+target.id+'/return',reader,'PUT'),()=>call('/borrows/'+target.id+'/return',reader,'PUT')]);
  const returned=(await success('/borrows/my',reader)).find(row=>row.id===target.id);
  const restocked=await success('/books/'+target.bookId,admin);
  assert(h3.responses.filter(r=>r.code===200).length===1&&returned.status==='RETURNED'&&restocked.stock===1,'H03重复释放或状态错误');h3.passed=true;h3.finalStock=restocked.stock;
  for(const row of (await success('/borrows/my',reader)).filter(row=>row.status==='BORROWING'))await success('/borrows/'+row.id+'/return',reader,'PUT');
  await success('/rules',admin,'PUT',ruleA);
  const h4=await overlap('H04改规则与借出',[()=>call('/rules',admin,'PUT',ruleB),()=>call('/borrows',reader,'POST',{bookId:books[4].id})]);
  const borrowed=(await success('/borrows/my',reader)).find(row=>row.bookId===books[4].id);
  const snapshot=[borrowed.borrowDaysSnapshot,borrowed.maxRenewCountSnapshot,borrowed.renewDaysSnapshot,Number(borrowed.finePerDaySnapshot)];
  const valid=JSON.stringify(snapshot)==='[3,2,7,0.5]'||JSON.stringify(snapshot)==='[10,1,3,1]';
  const days=(Date.parse(borrowed.dueTime.slice(0,10))-Date.parse(borrowed.borrowTime.slice(0,10)))/86400000;
  assert(h4.responses.every(r=>r.code===200)&&valid&&days===snapshot[0],'H04规则混组或期限错误');h4.passed=true;h4.snapshot=snapshot;
 } catch(error){results.push({error:error.message});process.exitCode=1}
 finally {
  try{if(reader)for(const row of (await success('/borrows/my',reader)).filter(row=>row.status==='BORROWING'))await success('/borrows/'+row.id+'/return',reader,'PUT');if(admin&&original)await success('/rules',admin,'PUT',original)}catch(error){results.push({cleanupError:error.message});process.exitCode=1}
  fs.writeFileSync('docs/并发测试结果.json',JSON.stringify({executedAt:new Date().toISOString(),iterations:1,results},null,2));
  console.log(JSON.stringify(results,null,2));
 }
})();
