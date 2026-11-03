// Dedicated regression runner. Never points SQL writes at the baseline database.
const fs=require('fs'),path=require('path'),cp=require('child_process'),crypto=require('crypto');
const {credential}=require('./qa-config.cjs');
const db='book_db_regression',base='http://127.0.0.1:8080/api';
const out=path.resolve('docs/2026-11-03回归证据'+(process.argv.includes('--rollback-only')?'/回滚补测':process.argv.includes('--supplement')?'/统计与边界补测':process.argv.includes('--concurrency-more')?'/并发组合补测':''));fs.mkdirSync(out,{recursive:true});
const records=[],triggers=[];let original,admin,seq=0;
const stamp=Date.now().toString(36),prefix='rg_'+stamp;
function sql(s,database=db){const text=s.startsWith('CREATE TRIGGER')?'DELIMITER //\n'+s+'//\nDELIMITER ;':s;const r=cp.spawnSync('C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe',['-h','localhost','-u',credential('username'),'--default-character-set=utf8mb4','-N','-B',database,'-e',text],{env:{...process.env,MYSQL_PWD:credential('password')},encoding:'utf8'});if(r.status!==0)throw Error('SQL执行失败（不输出连接凭据）：'+s.split(' ')[0]);return r.stdout.trim();}
const n=s=>Number(sql(s));
function save(){fs.writeFileSync(path.join(out,'接口与数据库结果.json'),JSON.stringify({startedAt,version,base,database:db,prefix,records},null,2));}
const startedAt=new Date().toISOString();
const version=cp.spawnSync('git',['-c','safe.directory=C:/图书管理系统/Library-Management-System','rev-parse','HEAD'],{encoding:'utf8'}).stdout.trim();
async function req(url,token=admin,method='GET',body){const start=new Date().toISOString(),t=performance.now();const r=await fetch(base+url,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body!==undefined?{body:JSON.stringify(body)}:{}),signal:AbortSignal.timeout(15000)});let result;try{result=await r.json()}catch{result={message:'非JSON响应'}}return {url,method,start,end:new Date().toISOString(),ms:Math.round(performance.now()-t),http:r.status,...result};}
function check(id,steps,expected,actual,ok){records.push({id,steps,expected,actual,status:ok?'通过':'失败'});save();console.log(id+' '+(ok?'通过':'失败'));}
async function test(id,steps,expected,fn){try{const [actual,ok]=await fn();check(id,steps,expected,actual,ok)}catch(e){check(id,steps,expected,{error:e.message},false)}}
const A={borrow_days:'30',max_renew_count:'2',renew_days:'7',fine_per_day:'0.50',max_borrow_count:'3'};
const B={borrow_days:'20',max_renew_count:'1',renew_days:'5',fine_per_day:'1.00',max_borrow_count:'3'};
const same=(a,b)=>Object.keys(a).every(k=>String(a[k])===String(b[k]));
async function rules(v){const r=await req('/rules',admin,'PUT',v);if(r.code!==200)throw Error('保存规则失败');return r;}
async function login(name){const r=await req('/auth/login',null,'POST',{username:name,password:'123456'});if(r.code!==200)throw Error('测试账号登录失败：'+name);return {token:r.data.token,id:r.data.user.id};}
function book(stock=5){const title=prefix+'_book_'+(++seq);sql(`INSERT INTO book(title,author,category_id,stock) VALUES ('${title}','回归夹具',${cat},${stock})`);return n(`SELECT id FROM book WHERE title='${title}'`);}
let cat,u,v;
function row(id){return sql('SELECT status,renew_count,due_time,fine_amount,overdue_days FROM borrow_record WHERE id='+id);}
async function borrow(b,user=u){const r=await req('/borrows',user.token,'POST',{bookId:b});if(r.code!==200)throw Error('借阅前置失败 '+r.message);return n(`SELECT MAX(id) FROM borrow_record WHERE user_id=${user.id} AND book_id=${b}`);}
function fixture(b,user=u){sql(`INSERT INTO borrow_record(user_id,book_id,borrow_time,due_time,status,renew_count,overdue_days,fine_amount,borrow_days_snapshot,max_renew_count_snapshot,renew_days_snapshot,fine_per_day_snapshot) VALUES (${user.id},${b},NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY),'BORROWING',0,0,0,30,2,7,0.50)`);return n(`SELECT MAX(id) FROM borrow_record WHERE book_id=${b}`);}
function clearActive(){sql(`UPDATE borrow_record SET status='RETURNED',return_time=NOW() WHERE user_id IN (${u.id},${v.id}) AND status='BORROWING'`);}
function trigger(name,definition){sql('CREATE TRIGGER '+name+' '+definition);triggers.push(name);}
function drop(name){sql('DROP TRIGGER IF EXISTS '+name);}
async function main(){
 if(!fs.readFileSync('springboot_back/src/main/resources/application-local.yml','utf8').includes('/'+db+'?'))throw Error('配置未指向独立测试库');
 // Connection-level proof before any HTTP mutation: active Spring connections must use regression.
 const connections=sql("SELECT COUNT(*) FROM information_schema.PROCESSLIST WHERE DB='book_db_regression'");
 if(Number(connections)<1)throw Error('未发现测试库活动连接，请确认后端已重启');
 admin=(await login('admin')).token;original=(await req('/rules')).data;
 check('ENV','核对local配置与MySQL活动连接','独立测试库且原规则30/2/7/0.50/3',{database:db,connections:Number(connections),rules:original},same(A,original));
 await rules(B);const targetRules=sql("SELECT config_value FROM system_config WHERE config_key='borrow_days'");const sourceRules=sql("SELECT config_value FROM system_config WHERE config_key='borrow_days'",'book_db_current');
 if(targetRules!=='20'||sourceRules!=='30')throw Error('运行服务数据库隔离核对失败');
 check('F02','管理员保存B，重新GET并由读者读取','五项整组更新；基线库仍30天',{response:(await req('/rules')).data,targetRules,sourceRules},same(B,(await req('/rules')).data));await rules(A);
 sql(`INSERT INTO category(name) VALUES ('${prefix}_category')`);cat=n(`SELECT id FROM category WHERE name='${prefix}_category'`);
 for(const suffix of ['a','b']){const name=prefix+'_'+suffix;sql(`INSERT INTO user(username,password,nickname,role,status) SELECT '${name}',password,'回归读者${suffix}','USER',1 FROM user WHERE username='user2'`);}
 u=await login(prefix+'_a');v=await login(prefix+'_b');
 if(process.argv.includes('--rollback-only')){await rollback();return;}
 if(process.argv.includes('--supplement')){await supplement();return;}
 if(process.argv.includes('--concurrency-more')){await concurrencyMore();return;}
 await test('A02-read','读者GET latest','可读取完整A',async()=>{const r=await req('/rules/latest',u.token);return[r,r.code===200&&same(A,r.data)]});
 const invalid={borrow_days:['0','91','1.5','abc',''],max_renew_count:['-1','6','1.5'],renew_days:['0','31','1.5'],fine_per_day:['-0.01','5.01','0.501'],max_borrow_count:['0','11','1.5']};
 for(const [key,values]of Object.entries(invalid))for(const val of [...values,null,undefined])await test('F03-'+key+'-'+String(val),'PUT五项规则，其中'+key+'='+String(val)+'；再读取','拒绝且五项不变',async()=>{const input={...A,[key]:val};const r=await req('/rules',admin,'PUT',input),after=(await req('/rules')).data;return[{response:r,after},r.code===400&&same(A,after)]});
 for(const [key,values]of Object.entries({borrow_days:['1','90'],max_renew_count:['0','5'],renew_days:['1','30'],fine_per_day:['0','5.00'],max_borrow_count:['1','10']}))for(const value of values)await test('F04-'+key+'-'+value,'PUT合法边界并重新读取','成功且一致',async()=>{const x={...A,[key]:value},r=await req('/rules',admin,'PUT',x),after=(await req('/rules')).data;await rules(A);return[{response:r,after},r.code===200&&Number(after[key])===Number(value)]});
 for(const [id,url,tok,method,body]of [['A01-reader','/statistics/overview',u.token],['A01-anon','/statistics/overview',null],['A01-invalid','/statistics/overview','invalid'],['A02-edit','/rules',u.token,'PUT',B],['A02-anon','/rules/latest',null],['A02-admin-read','/rules',u.token]])await test(id,'直接请求'+url,'权限拒绝，不含业务数据',async()=>{const r=await req(url,tok,method,body);return[r,[401,403].includes(r.code)&&r.data==null]});
 const b1=book(),id=await borrow(b1);await rules(B);
 await test('B01','A下真实借出后改B，核对DB快照','30/2/7/0.50且到期+30天',async()=>{const r=sql(`SELECT borrow_days_snapshot,max_renew_count_snapshot,renew_days_snapshot,fine_per_day_snapshot,TIMESTAMPDIFF(DAY,borrow_time,due_time) FROM borrow_record WHERE id=${id}`);return[r,r==='30\t2\t7\t0.50\t30']});
 await test('B02','当前B下续借A记录','旧快照续借7天且次数1',async()=>{const r=await req('/borrows/'+id+'/renew',u.token,'PUT'),delta=n(`SELECT TIMESTAMPDIFF(DAY,borrow_time,due_time) FROM borrow_record WHERE id=${id}`);return[{response:r,row:row(id),delta},r.code===200&&delta===37]});
 await req('/borrows/'+id+'/renew',u.token,'PUT');await test('B03','A记录第三次续借','拒绝且记录不变',async()=>{const before=row(id),r=await req('/borrows/'+id+'/renew',u.token,'PUT');return[{response:r,before,after:row(id)},r.code===400&&before===row(id)]});
 for(const action of ['renew','return'])await test('A03-'+action,'乙操作甲的借阅','403且记录不变',async()=>{const before=row(id),r=await req('/borrows/'+id+'/'+action,v.token,'PUT');return[{response:r,after:row(id)},r.code===403&&row(id)===before]});
 sql(`UPDATE borrow_record SET due_time=DATE_SUB(CURDATE(),INTERVAL 2 DAY) WHERE id=${id}`);
 await test('B06','构造逾期2天，查询未还记录','逾期2天/暂计1元且DB最终金额未写入',async()=>{const r=(await req('/borrows/my',u.token)).data.find(x=>x.id===id);return[{record:r,db:row(id)},r.overdueDays===2&&Number(r.fineAmount)===1&&row(id).split('\t')[3]==='0.00']});
 await test('B07','当前日费1元，真实归还历史日费0.50的逾期2天记录','最终1元，库存恢复',async()=>{const r=await req('/borrows/'+id+'/return',u.token,'PUT');return[{response:r,db:row(id),stock:n('SELECT stock FROM book WHERE id='+b1)},r.code===200&&row(id).startsWith('RETURNED')&&row(id).split('\t')[3]==='1.00'&&n('SELECT stock FROM book WHERE id='+b1)===5]});
 await rules(A);await test('B08','再次改规则后读取已还记录','最终金额固定1元',async()=>{const r=(await req('/borrows/my',u.token)).data.find(x=>x.id===id);return[r,Number(r.fineAmount)===1]});
 await test('B-return-repeat','重复归还','拒绝且库存不变',async()=>{const before=row(id),r=await req('/borrows/'+id+'/return',u.token,'PUT');return[{response:r,db:row(id)},r.code===400&&before===row(id)&&n('SELECT stock FROM book WHERE id='+b1)===5]});
 await rules({...A,max_renew_count:'0',fine_per_day:'0'});const zero=await borrow(book());await test('B03-zero','零续借快照首次续借','拒绝',async()=>{const r=await req('/borrows/'+zero+'/renew',u.token,'PUT');return[r,r.code===400]});sql(`UPDATE borrow_record SET due_time=DATE_SUB(CURDATE(),INTERVAL 2 DAY) WHERE id=${zero}`);
 await test('B09','零日费逾期2天查询并真实归还','仍逾期2天，最终0元',async()=>{const x=(await req('/borrows/my',u.token)).data.find(x=>x.id===zero),r=await req('/borrows/'+zero+'/return',u.token,'PUT');return[{record:x,response:r,db:row(zero)},x.overdueDays===2&&Number(x.fineAmount)===0&&r.code===200&&row(zero).endsWith('0.00\t2')]});await rules(A);
 for(const q of ['start=2026-10-01','end=2026-10-31','start=2026-10-31&end=2026-10-01','start=bad&end=bad'])await test('S03-'+q,'GET overview?'+q,'400',async()=>{const r=await req('/statistics/overview?'+q);return[r,r.code===400||r.http===400]});
 for(const p of ['dashboard','category','top-books'])await test('S07-'+p,'有效管理员请求旧统计入口','404',async()=>{const r=await req('/statistics/'+p);return[r,r.http===404||r.code===404]});
 const sb=book();for(const date of ['2020-01-01 00:00:00','2020-01-31 23:59:59','2020-02-01 00:00:00']){const f=fixture(sb);sql(`UPDATE borrow_record SET borrow_time='${date}',status='RETURNED',return_time=due_time WHERE id=${f}`);}
 await test('S02','查询2020年1月首尾边界夹具','该书次数2；零数据范围保留类别',async()=>{const r=await req('/statistics/overview?start=2020-01-01&end=2020-01-31'),empty=await req('/statistics/overview?start=1900-01-01&end=1900-01-01');return[{range:r,empty},r.data.topBooks.find(x=>x.bookId===sb)?.borrowCount===2&&empty.data.topBooks.length===0&&empty.data.categoryStats.every(x=>x.borrowCount===0)]});
 // Real reservation flow, each actor uses independent identity.
 clearActive();const pb=book(1),owner=await borrow(pb,v);await req('/reservations',u.token,'POST',{bookId:pb});await req('/borrows/'+owner+'/return',v.token,'PUT');const reservation=n(`SELECT MAX(id) FROM reservation WHERE user_id=${u.id} AND book_id=${pb}`);await rules(B);
 await test('B04','乙借最后一本→甲预约→乙归还→改B→甲取书','READY转FULFILLED，新快照20/1/5/1且不重复扣库存',async()=>{const ready=sql('SELECT status FROM reservation WHERE id='+reservation),r=await req('/reservations/'+reservation+'/pickup',u.token,'PUT'),s=sql(`SELECT borrow_days_snapshot,max_renew_count_snapshot,renew_days_snapshot,fine_per_day_snapshot FROM borrow_record WHERE user_id=${u.id} AND book_id=${pb} ORDER BY id DESC LIMIT 1`);return[{ready,response:r,snapshot:s,stock:n('SELECT stock FROM book WHERE id='+pb)},ready==='READY'&&r.code===200&&s==='20\t1\t5\t1.00'&&n('SELECT stock FROM book WHERE id='+pb)===0]});await rules(A);
 // Targeted trigger pauses create server-side overlap AFTER both check predicates, before insert/update.
 for(const scenario of ['limit','duplicate','stock','renew','return','pickup'])for(let round=1;round<=5;round++)await test('C-'+scenario+'-'+round,'受控触发器延迟0.5秒，Promise.all并行两请求；每轮重建夹具','名额/重复借阅/库存/续借/归还/取书保持一致',async()=>{
   clearActive();await rules({...A,max_borrow_count:'1'});const b=book(scenario==='stock'?1:5);let specs,triggerTable='borrow_record',event='INSERT',beforeId;
   if(['renew','return'].includes(scenario)){beforeId=fixture(b);event='UPDATE';specs=[()=>req('/borrows/'+beforeId+'/'+scenario,u.token,'PUT'),()=>req('/borrows/'+beforeId+'/'+scenario,u.token,'PUT')];if(scenario==='renew')sql('UPDATE borrow_record SET renew_count=1 WHERE id='+beforeId);}
   else if(scenario==='pickup'){sql(`INSERT INTO reservation(user_id,book_id,status,create_time,expire_time,update_time) VALUES (${u.id},${b},'READY',NOW(),DATE_ADD(NOW(),INTERVAL 3 DAY),NOW())`);const rid=n(`SELECT MAX(id) FROM reservation WHERE book_id=${b}`);specs=[()=>req('/reservations/'+rid+'/pickup',u.token,'PUT'),()=>req('/reservations/'+rid+'/pickup',u.token,'PUT')];}
   else{const b2=scenario==='limit'?book():b;specs=[()=>req('/borrows',u.token,'POST',{bookId:b}),()=>req('/borrows',scenario==='stock'?v.token:u.token,'POST',{bookId:b2})];}
   const name='rg_overlap_'+stamp;trigger(name,`BEFORE ${event} ON ${triggerTable} FOR EACH ROW DO SLEEP(0.5)`);let responses;try{responses=await Promise.all(specs.map(f=>f()))}finally{drop(name)}
   const active=n(`SELECT COUNT(*) FROM borrow_record WHERE user_id=${u.id} AND status='BORROWING'`),count=n('SELECT COUNT(*) FROM borrow_record WHERE book_id='+b),stock=n('SELECT stock FROM book WHERE id='+b);let ok;
   if(scenario==='renew')ok=n('SELECT renew_count FROM borrow_record WHERE id='+beforeId)===2&&n('SELECT TIMESTAMPDIFF(DAY,borrow_time,due_time) FROM borrow_record WHERE id='+beforeId)===37;
   else if(scenario==='return')ok=stock===6&&responses.filter(x=>x.code===200).length===1; // fixture inserted directly, starting available stock=5
   else if(scenario==='stock')ok=count===1&&stock===0;
   else if(scenario==='pickup')ok=count===1;
   else ok=active===1&&responses.filter(x=>x.code===200).length===1;
   return[{responses,active,count,stock,record:beforeId?row(beforeId):null,trigger:'测试库BEFORE '+event+' SLEEP(0.5)，已移除'},ok];
 });
 clearActive();await rules(A);
 await rollback();
}
async function rollback(){
 for(const scenario of ['borrow','return','pickup','rules'])await test('R-'+scenario,'在测试库必要写入位置注入SIGNAL故障','HTTP业务失败且所有相关变更回滚',async()=>{
   clearActive();let table,event,operation,b=book(),id,before;
   if(scenario==='borrow'){table='book';event='UPDATE';operation=()=>req('/borrows',u.token,'POST',{bookId:b});before=n('SELECT COUNT(*) FROM borrow_record WHERE book_id='+b);}
   if(scenario==='return'){id=fixture(b);table='book';event='UPDATE';operation=()=>req('/borrows/'+id+'/return',u.token,'PUT');before=row(id);}
   if(scenario==='pickup'){sql(`INSERT INTO reservation(user_id,book_id,status,create_time,expire_time,update_time) VALUES (${u.id},${b},'READY',NOW(),DATE_ADD(NOW(),INTERVAL 3 DAY),NOW())`);id=n('SELECT MAX(id) FROM reservation WHERE book_id='+b);table='reservation';event='UPDATE';operation=()=>req('/reservations/'+id+'/pickup',u.token,'PUT');before=0;}
   if(scenario==='rules'){table='system_config';event='UPDATE';operation=()=>req('/rules',admin,'PUT',B);before=(await req('/rules')).data;}
   const name='rg_fault_'+stamp;const condition=scenario==='rules'?"IF NEW.config_key='renew_days' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RG injected failure'; END IF;":"SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='RG injected failure';";
   trigger(name,`BEFORE ${event} ON ${table} FOR EACH ROW BEGIN ${condition} END`);let r;try{r=await operation()}finally{drop(name)}
   const after=scenario==='rules'?(await req('/rules')).data:scenario==='return'?row(id):n('SELECT COUNT(*) FROM borrow_record WHERE book_id='+b);
   const ok=r.code!==200&&(scenario==='rules'?same(before,after):before===after)&&(scenario!=='pickup'||sql('SELECT status FROM reservation WHERE id='+id)==='READY');return[{response:r,before,after,stock:n('SELECT stock FROM book WHERE id='+b)},ok];
 });
}
async function supplement(){
 clearActive();await rules(A);
 const ids=Array.from({length:12},()=>book()),expected=[];
 for(let i=0;i<12;i++){const count=i<9?12-i:3;expected.push({bookId:ids[i],borrowCount:count});for(let j=0;j<count;j++){const id=fixture(ids[i]);sql(`UPDATE borrow_record SET borrow_time='2019-06-15 12:00:00',status='RETURNED',return_time=due_time WHERE id=${id}`);}}
 const zero=book();
 await test('S04','12种图书，9种降序、末3种并列3次，另有零借阅图书；查询2019年6月','TOP10按次数降序/ID升序，零次不入榜',async()=>{const r=await req('/statistics/overview?start=2019-06-01&end=2019-06-30'),actual=r.data.topBooks.map(x=>({bookId:x.bookId,borrowCount:x.borrowCount}));return[{expected:expected.slice(0,10),actual,zeroBook:zero},JSON.stringify(actual)===JSON.stringify(expected.slice(0,10))]});
 await test('S06','对照真实DB全局概况和独立罚金汇总','五项概况、最终+未还暂计罚金一致',async()=>{const r=(await req('/statistics/overview')).data.dashboard;const values=sql("SELECT (SELECT COUNT(*) FROM book),(SELECT SUM(stock) FROM book),(SELECT COUNT(*) FROM borrow_record WHERE status='BORROWING'),(SELECT COUNT(*) FROM borrow_record WHERE status='BORROWING' AND DATE(due_time)<CURDATE()),(SELECT COUNT(*) FROM user WHERE role='USER'),(SELECT COALESCE(SUM(CASE WHEN status='RETURNED' THEN fine_amount WHEN status='BORROWING' THEN GREATEST(DATEDIFF(CURDATE(),due_time),0)*fine_per_day_snapshot ELSE 0 END),0) FROM borrow_record)").split('\t').map(Number);return[{response:r,expected:values},['totalBooks','totalStock','borrowingCount','overdueCount','userCount','totalFine'].every((k,i)=>Number(r[k])===values[i])]});
 const b=book();let id;
 const contribution=async()=>{const r=await req('/statistics/overview');return r.data.categoryStats.find(x=>x.categoryName===prefix+'_category').borrowCount;};const before=await contribution(),counts=[];
 id=await borrow(b);counts.push(await contribution());await req('/borrows/'+id+'/renew',u.token,'PUT');counts.push(await contribution());await req('/borrows/'+id+'/return',u.token,'PUT');counts.push(await contribution());await borrow(b);counts.push(await contribution());check('S05','真实借出→续借→归还→再借，统计分类次数变化','+1/+1/+1/+2',{before,counts},JSON.stringify(counts)===JSON.stringify([before+1,before+1,before+1,before+2]));
 clearActive();await rules({...A,max_borrow_count:'1'});await borrow(book());const refused=book();await test('B05','已达1本上限，再借另一本','拒绝且不扣库存、不插记录',async()=>{const r=await req('/borrows',u.token,'POST',{bookId:refused});return[{response:r,stock:n('SELECT stock FROM book WHERE id='+refused)},r.code===400&&n('SELECT stock FROM book WHERE id='+refused)===5&&n('SELECT COUNT(*) FROM borrow_record WHERE book_id='+refused)===0]});
 clearActive();await rules(A);
 for(const days of [0,1,2]){const id=fixture(book());sql(`UPDATE borrow_record SET due_time=DATE_SUB(CURDATE(),INTERVAL ${days} DAY) WHERE id=${id}`);await test('B06-days-'+days,'到期日为今天减'+days+'天，读取记录','逾期'+days+'天，金额'+(days*0.5),async()=>{const x=(await req('/borrows/my',u.token)).data.find(x=>x.id===id);return[x,x.overdueDays===days&&Number(x.fineAmount)===days*0.5]});}
 await test('A03-my','甲请求my?userId=乙ID','仍仅返回甲记录',async()=>{const r=await req('/borrows/my?userId='+v.id,u.token);return[r,r.code===200&&r.data.every(x=>x.userId===u.id)]});
 clearActive();
 const bready=book(0);sql(`INSERT INTO reservation(user_id,book_id,status,create_time,expire_time,update_time) VALUES (${u.id},${bready},'READY',NOW(),DATE_ADD(NOW(),INTERVAL 3 DAY),NOW())`);const rid=n('SELECT MAX(id) FROM reservation WHERE book_id='+bready);await rules({...A,max_borrow_count:'1'});await borrow(book());
 await test('B05-pickup','满额读者领取READY预约','拒绝且预约/记录不变',async()=>{const r=await req('/reservations/'+rid+'/pickup',u.token,'PUT'),state=sql('SELECT status FROM reservation WHERE id='+rid);return[{response:r,state},r.code===400&&state==='READY'&&n('SELECT COUNT(*) FROM borrow_record WHERE book_id='+bready)===0]});
 await test('A03-pickup','乙领取甲预约','403且预约不变',async()=>{const r=await req('/reservations/'+rid+'/pickup',v.token,'PUT');return[r,r.code===403&&sql('SELECT status FROM reservation WHERE id='+rid)==='READY']});
 clearActive();
}
async function concurrencyMore(){
 const admin2=(await login('admin')).token;
 for(const scenario of ['save-borrow','save-pickup','renew-return','borrow-pickup','save-save'])for(let round=1;round<=5;round++)await test('C-'+scenario+'-'+round,'每轮独立夹具，测试库触发器延迟制造交错','规则组完整，业务结果可串行解释且不超限',async()=>{
   clearActive();await rules({...A,max_borrow_count:'1'});const b=book(scenario.includes('pickup')?0:5);let rid,id;
   if(scenario.includes('pickup')){sql(`INSERT INTO reservation(user_id,book_id,status,create_time,expire_time,update_time) VALUES (${u.id},${b},'READY',NOW(),DATE_ADD(NOW(),INTERVAL 3 DAY),NOW())`);rid=n('SELECT MAX(id) FROM reservation WHERE book_id='+b);}
   if(scenario==='renew-return')id=fixture(b);
   const name='rg_extra_'+stamp,config=scenario.startsWith('save');
   trigger(name,config?'BEFORE UPDATE ON system_config FOR EACH ROW DO SLEEP(0.15)':scenario==='renew-return'?'BEFORE UPDATE ON borrow_record FOR EACH ROW DO SLEEP(0.5)':'BEFORE INSERT ON borrow_record FOR EACH ROW DO SLEEP(0.5)');
   let specs;if(scenario==='save-borrow')specs=[()=>req('/rules',admin,'PUT',B),()=>req('/borrows',u.token,'POST',{bookId:b})];
   if(scenario==='save-pickup')specs=[()=>req('/rules',admin,'PUT',B),()=>req('/reservations/'+rid+'/pickup',u.token,'PUT')];
   if(scenario==='renew-return')specs=[()=>req('/borrows/'+id+'/renew',u.token,'PUT'),()=>req('/borrows/'+id+'/return',u.token,'PUT')];
   if(scenario==='borrow-pickup'){const b2=book();specs=[()=>req('/borrows',u.token,'POST',{bookId:b2}),()=>req('/reservations/'+rid+'/pickup',u.token,'PUT')];}
   if(scenario==='save-save')specs=[()=>req('/rules',admin,'PUT',B),()=>req('/rules',admin2,'PUT',A)];
   let responses;try{responses=await Promise.all(specs.map(f=>f()))}finally{drop(name)}
   const current=(await req('/rules')).data,snap=sql(`SELECT borrow_days_snapshot,max_renew_count_snapshot,renew_days_snapshot,fine_per_day_snapshot,TIMESTAMPDIFF(DAY,borrow_time,due_time) FROM borrow_record WHERE book_id=${b} ORDER BY id DESC LIMIT 1`),active=n(`SELECT COUNT(*) FROM borrow_record WHERE user_id=${u.id} AND status='BORROWING'`);
   let ok;if(scenario==='save-save')ok=(same(A,current)||same(B,current))&&responses.every(r=>r.code===200);
   else if(config)ok=['30\t2\t7\t0.50\t30','20\t1\t5\t1.00\t20'].includes(snap)&&responses.every(r=>r.code===200);
   else if(scenario==='borrow-pickup')ok=active===1&&responses.filter(r=>r.code===200).length===1;
   else ok=row(id).startsWith('RETURNED')&&n('SELECT stock FROM book WHERE id='+b)===6&&n('SELECT renew_count FROM borrow_record WHERE id='+id)<=1;
   return[{bookId:b,reservationId:rid,borrowId:id,responses,rules:current,snapshot:snap,active,record:id?row(id):null,triggerRemoved:true},ok];
 });clearActive();
}
main().catch(e=>{check('RUN-ERROR','执行回归','执行完成',{error:e.message},false);process.exitCode=1}).finally(async()=>{
 for(const t of triggers)try{drop(t)}catch{}
 if(admin&&original)try{await rules(original);check('RESTORE','恢复测试前五项规则','30/2/7/0.50/3',(await req('/rules')).data,same(original,(await req('/rules')).data));}catch(e){check('RESTORE','恢复规则','成功',e.message,false)}
 save();console.log(JSON.stringify({cases:records.length,passed:records.filter(x=>x.status==='通过').length,failed:records.filter(x=>x.status==='失败').map(x=>x.id),out}));
});
