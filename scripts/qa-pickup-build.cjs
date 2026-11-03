const fs=require('fs');const source=fs.readFileSync('scripts/qa-concurrency.cjs','utf8');const prefix=source.slice(0,source.indexOf('(async()=>{'));
const body=`(async()=>{let admin,owner,receiver,original,book;try{
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
finally{try{if(book)for(const token of [owner,receiver])if(token)for(const row of(await success('/borrows/my',token)).filter(r=>r.bookId===book.id&&r.status==='BORROWING'))await success('/borrows/'+row.id+'/return',token,'PUT');if(admin&&original)await success('/rules',admin,'PUT',original)}catch(error){results.push({cleanupError:error.message});process.exitCode=1}fs.writeFileSync('docs/预约取书并发结果.json',JSON.stringify(results,null,2));console.log(JSON.stringify(results,null,2))}})();`;
fs.writeFileSync('scripts/qa-pickup-concurrency.cjs',prefix+body);
