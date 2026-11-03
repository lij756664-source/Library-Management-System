// Read-only statistical load after creating isolated ADMIN fixtures in the dedicated performance schema.
const fs=require('fs'),cp=require('child_process'),crypto=require('crypto');
const {credential}=require('./qa-config.cjs');
const dir='docs/2026-11-03多人压力测试',base='http://127.0.0.1:8082/api';
fs.mkdirSync(dir,{recursive:true});
if(fs.existsSync(dir+'/请求记录.jsonl'))throw Error('已存在压力证据，停止避免覆盖');
const expected=JSON.parse(fs.readFileSync('docs/2026-11-03性能测试/独立预期.json','utf8'));
const oldBuild=JSON.parse(fs.readFileSync('docs/2026-11-03性能测试/逐项校验结果.json','utf8'));
const jarHash=crypto.createHash('sha256').update(fs.readFileSync('springboot_back/target/book-management-1.0.0.jar')).digest('hex');
if(jarHash!==oldBuild.productionJarSha256)throw Error('构建已变化，需重新核对');
function sql(q){const r=cp.spawnSync('C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe',['-h','localhost','-u',credential('username'),'--default-character-set=utf8mb4','-N','-B','book_db_perf_20261103','-e',q],{env:{...process.env,MYSQL_PWD:credential('password')},encoding:'utf8',maxBuffer:8*1024*1024});if(r.status!==0)throw Error('性能库SQL失败');return r.stdout.trim();}
const wait=ms=>new Promise(r=>setTimeout(r,ms));
const stamp=Date.now().toString(36),requests=[],phases=[];let state={},dashboardExpected;
function status(extra={}){state={...state,...extra,at:new Date().toISOString(),completed:requests.length};fs.writeFileSync(dir+'/运行状态.json',JSON.stringify(state,null,2));}
function compare(body,range){if(body.code!==200||!body.data)return false;const d=body.data;const sort=a=>[...a].sort((x,y)=>x.categoryName.localeCompare(y.categoryName,'zh-CN'));
return JSON.stringify(d.topBooks.map(x=>({bookTitle:x.bookTitle,borrowCount:x.borrowCount})))===JSON.stringify(range.expected.top)&&JSON.stringify(sort(d.categoryStats.map(x=>({categoryName:x.categoryName,borrowCount:x.borrowCount}))))===JSON.stringify(sort(range.expected.categories))&&Object.keys(dashboardExpected).every(k=>Number(d.dashboard[k])===dashboardExpected[k]);}
async function query(user,range,phase){const q=range.start?'?start='+range.start+'&end='+range.end:'';const startedAt=new Date().toISOString(),t=performance.now();let http,code,correct=false,error;
try{const r=await fetch(base+'/statistics/overview'+q,{headers:{Authorization:'Bearer '+user.token},signal:AbortSignal.timeout(15000)});http=r.status;const body=await r.json();code=body.code;correct=compare(body,range);}catch(e){error=e.name+': '+e.message;}
const item={phase,user:user.name,range:range.name,startedAt,endedAt:new Date().toISOString(),ms:Math.round((performance.now()-t)*100)/100,http,code,correct,success:http===200&&code===200,...(error?{error}:{})};requests.push(item);fs.appendFileSync(dir+'/请求记录.jsonl',JSON.stringify(item)+'\n');return item;}
function summary(name){const rows=requests.filter(x=>x.phase===name),times=rows.map(x=>x.ms).sort((a,b)=>a-b),p=x=>times[Math.max(0,Math.ceil(times.length*x)-1)];return {name,total:rows.length,success:rows.filter(x=>x.success).length,incorrect:rows.filter(x=>!x.correct).length,failures:rows.filter(x=>!x.success).length,p50Ms:p(.5),p95Ms:p(.95),p99Ms:p(.99),maxMs:times.at(-1),perRange:expected.ranges.map(r=>{const a=rows.filter(x=>x.range===r.name),t=a.map(x=>x.ms).sort((x,y)=>x-y);return {range:r.name,total:a.length,p95Ms:t[Math.max(0,Math.ceil(t.length*.95)-1)],maxMs:t.at(-1)};})};}
async function phase(name,users,seconds){const start=Date.now(),until=start+seconds*1000;status({phase:name,users:users.length,start:new Date(start).toISOString(),scheduledEnd:new Date(until).toISOString()});console.log('START '+name+' '+seconds+'s');
await Promise.all(users.map(async(user,index)=>{let iteration=0,seed=index+17;while(Date.now()<until){await query(user,expected.ranges[(index+iteration++)%5],name);seed=(seed*16807)%2147483647;const delay=2000+(seed%3001);if(Date.now()+delay>=until)break;await wait(delay);} }));const end=Date.now();
if(end<until)await wait(until-end);const result={...summary(name),users:users.length,startedAt:new Date(start).toISOString(),endedAt:new Date().toISOString(),durationSeconds:(Date.now()-start)/1000};result.throughputPerSecond=Number((result.total/result.durationSeconds).toFixed(3));phases.push(result);fs.writeFileSync(dir+'/阶段结果.json',JSON.stringify(phases,null,2));console.log(JSON.stringify(result));}
async function main(){
const today=sql('SELECT CURDATE()');const rows=sql('SELECT status,DATE(due_time),fine_per_day_snapshot,fine_amount FROM borrow_record').split(/\r?\n/).map(x=>x.split('\t'));let fineCents=0,overdue=0,active=0;
for(const [s,d,rate,final]of rows){if(s==='RETURNED')fineCents+=Math.round(Number(final)*100);else if(s==='BORROWING'){active++;const days=Math.max(0,Math.round((Date.parse(today)-Date.parse(d))/86400000));if(days)overdue++;fineCents+=days*Math.round(Number(rate)*100);}}
dashboardExpected={totalBooks:Number(sql('SELECT COUNT(*) FROM book')),totalStock:Number(sql('SELECT SUM(stock) FROM book')),borrowingCount:active,overdueCount:overdue,totalFine:fineCents/100,userCount:Number(sql("SELECT COUNT(*) FROM user WHERE role='USER'"))};
const users=[];for(let i=1;i<=20;i++){const name='load_'+stamp+'_'+i;sql(`INSERT INTO user(username,password,nickname,role,status) SELECT '${name}',password,'压力测试管理员','ADMIN',1 FROM user WHERE username='admin'; INSERT INTO user_role(user_id,role_id) SELECT u.id,r.id FROM user u JOIN role r ON r.code='ADMIN' WHERE u.username='${name}'`);const r=await(await fetch(base+'/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:name,password:'123456'})})).json();if(r.code!==200)throw Error('压力账号登录失败');users.push({name,token:r.data.token});}
fs.writeFileSync(dir+'/环境与方案.json',JSON.stringify({date:'2026-11-03',base,database:'book_db_perf_20261103',jarSha256:jarHash,scale:expected.scale,dashboardExpected,workload:'每个用户顺序查询5种范围，完成后等待2—5秒；各范围轮转',users:users.map(x=>x.name),thresholds:{users:10,durationSeconds:300,p95Ms:2000,successRate:1,correctnessRate:1,browserSamples:10,browserMaxMs:3000},phases:[{name:'ramp-5',users:5,seconds:20},{name:'acceptance-10',users:10,seconds:300},{name:'explore-20',users:20,seconds:120}]},null,2));
for(const r of expected.ranges){const x=await query(users[0],r,'precheck');if(!x.success||!x.correct)throw Error('预检查失败');}
await phase('ramp-5',users.slice(0,5),20);await phase('acceptance-10',users.slice(0,10),300);await phase('explore-20',users,120);
for(const range of expected.ranges)await query(users[0],range,'recovery');
status({phase:'complete'});console.log('LOAD COMPLETE');
}
main().catch(e=>{status({phase:'error',error:e.message});console.error(e.message);process.exitCode=1;});
