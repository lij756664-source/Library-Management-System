const fs=require('fs');
(async()=>{
const api='http://127.0.0.1:8090/api',checks=[];
async function req(path,token,method='GET',body){const r=await fetch(api+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body!==undefined?{body:JSON.stringify(body)}:{})});return r.json();}
const check=(name,ok)=>{checks.push({name,passed:!!ok});if(!ok)throw Error(name)};
const login=async username=>{const r=await req('/auth/login',null,'POST',{username,password:'123456'});check(username+' login',r.code===200);return r.data.token};
const admin=await login('admin'),reader=await login('user2');
const latest=await req('/rules/latest',reader);check('reader latest rules',latest.code===200&&String(latest.data.borrow_days)==='30');
check('reader rules editing blocked',(await req('/rules',reader)).code===403);
check('anonymous books blocked',(await req('/books')).code===401);
const books=await req('/books?availableOnly=true',reader);check('available filter with location',books.code===200&&books.data.length>0&&books.data.every(b=>b.stock>0&&'location'in b));
check('book details location',(await req('/books/1',reader)).data.location.includes('A-01'));
check('reader stock maintenance blocked',(await req('/books/1/stock',reader,'PUT',{delta:1})).code===403);
const initial=(await req('/books/1',admin)).data.stock;
check('admin stock increment',(await req('/books/1/stock',admin,'PUT',{delta:1})).data===initial+1);
check('admin stock restore',(await req('/books/1/stock',admin,'PUT',{delta:-1})).data===initial);
const before=(await req('/statistics/overview',admin)).data.topBooks.reduce((s,b)=>s+Number(b.borrowCount),0);
check('borrow',(await req('/borrows',reader,'POST',{bookId:1})).code===200);
let row=(await req('/borrows/my',reader)).data.find(r=>r.bookId===1&&r.status==='BORROWING');
check('new rule snapshot',row&&row.borrowDaysSnapshot===30&&row.maxRenewCountSnapshot===2&&row.renewDaysSnapshot===7&&Number(row.finePerDaySnapshot)===0.5);
check('renew',(await req('/borrows/'+row.id+'/renew',reader,'PUT')).code===200);
check('return',(await req('/borrows/'+row.id+'/return',reader,'PUT')).code===200);
row=(await req('/borrows/my',reader)).data.find(r=>r.id===row.id);check('returned final zero fine',row.status==='RETURNED'&&Number(row.fineAmount)===0);
const after=await req('/statistics/overview',admin);check('borrow counts once after renew and return',after.code===200&&after.data.topBooks.reduce((s,b)=>s+Number(b.borrowCount),0)===before+1);
check('stock remains correct after return',(await req('/books/1',admin)).data.stock===initial);
check('single date rejected',(await req('/statistics/overview?start=2026-11-02',admin)).code===400);
fs.writeFileSync('docs/整合接口验证.json',JSON.stringify({testedAt:new Date().toISOString(),database:'book_db_integration_20261102c',checks,passed:checks.every(c=>c.passed)},null,2));console.log(JSON.stringify(checks));
})().catch(e=>{console.error(e.message);process.exitCode=1});
