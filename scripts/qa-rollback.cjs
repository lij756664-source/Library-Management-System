const fs=require('fs'),{execFileSync}=require('child_process');
const { credential } = require('./qa-config.cjs');
const env={...process.env,MYSQL_PWD:credential('password')};
const sql=q=>execFileSync('C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe',['--default-character-set=utf8mb4','-h','127.0.0.1','-u',credential('username'),'-N','-B','book_db_current','-e',q.startsWith('CREATE TRIGGER') ? ('DELIMITER //'+String.fromCharCode(10)+q+'//'+String.fromCharCode(10)+'DELIMITER ;') : q],{env,encoding:'utf8'}).trim();
const results=[];const api='http://127.0.0.1:8080/api';
async function call(path,token,method='GET',body){return(await fetch(api+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body===undefined?{}:{body:JSON.stringify(body)})})).json()}
function check(ok,m){if(!ok)throw Error(m)}
async function ok(path,t,m,b){const r=await call(path,t,m,b);check(r.code===200,path+': '+r.message);return r.data}
(async()=>{let admin,reader,original,target,book;const triggers=['qa_rule_fail','qa_return_fail','qa_stock_fail'];
try{
check(sql("SELECT COUNT(*) FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='book_db_current' AND TRIGGER_NAME IN ('qa_rule_fail','qa_return_fail','qa_stock_fail');")==='0','存在同名触发器，停止');
admin=(await ok('/auth/login',null,'POST',{username:'admin',password:'123456'})).token;
reader=(await ok('/auth/login',null,'POST',{username:'qa_parallel',password:'123456'})).token;
original=await ok('/rules',admin);
sql("CREATE TRIGGER qa_rule_fail BEFORE UPDATE ON system_config FOR EACH ROW BEGIN IF OLD.config_key='renew_days' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA third rule write failure'; END IF; END");
const save=await call('/rules',admin,'PUT',{borrow_days:'11',max_renew_count:'1',renew_days:'9',fine_per_day:'1.25',max_borrow_count:'4'});
const after=await ok('/rules',admin);check(save.code!==200&&Object.keys(original).every(k=>original[k]===after[k]),'G01部分规则生效');results.push({name:'G01第三项规则写入失败',passed:true,responseCode:save.code,allRulesUnchanged:true});sql('DROP TRIGGER qa_rule_fail');
const books=await ok('/books?keyword='+encodeURIComponent('QA并发'),admin);book=books.find(b=>b.stock>0);check(book,'缺少独立测试图书');await ok('/borrows',reader,'POST',{bookId:book.id});target=(await ok('/borrows/my',reader)).find(r=>r.bookId===book.id&&r.status==='BORROWING');
function verifyRollback(name,response){const state=sql('SELECT status,IFNULL(return_time,\'NULL\'),overdue_days,fine_amount FROM borrow_record WHERE id='+target.id);const stock=sql('SELECT stock FROM book WHERE id='+book.id);check(response.code!==200&&state.startsWith('BORROWING\tNULL\t')&&stock==='0',name+'未回滚');results.push({name,passed:true,responseCode:response.code,state,stock:Number(stock)})}
sql('CREATE TRIGGER qa_return_fail BEFORE UPDATE ON borrow_record FOR EACH ROW BEGIN IF OLD.id='+target.id+" AND NEW.status='RETURNED' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA return write failure'; END IF; END");
verifyRollback('G05归还记录保存失败',await call('/borrows/'+target.id+'/return',reader,'PUT'));sql('DROP TRIGGER qa_return_fail');
sql('CREATE TRIGGER qa_stock_fail BEFORE UPDATE ON book FOR EACH ROW BEGIN IF OLD.id='+book.id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA stock write failure'; END IF; END");
verifyRollback('G06归还后库存回补失败',await call('/borrows/'+target.id+'/return',reader,'PUT'));sql('DROP TRIGGER qa_stock_fail');
await ok('/borrows/'+target.id+'/return',reader,'PUT');target=null;
}catch(error){results.push({error:error.message});process.exitCode=1}
finally{for(const name of triggers){try{sql('DROP TRIGGER IF EXISTS '+name)}catch(error){results.push({cleanupError:name});process.exitCode=1}}try{if(target&&reader)await ok('/borrows/'+target.id+'/return',reader,'PUT');if(admin&&original)await ok('/rules',admin,'PUT',original)}catch(error){results.push({cleanupError:error.message});process.exitCode=1}fs.writeFileSync('docs/回滚测试结果.json',JSON.stringify({executedAt:new Date().toISOString(),results},null,2));console.log(JSON.stringify(results,null,2))}
})();
