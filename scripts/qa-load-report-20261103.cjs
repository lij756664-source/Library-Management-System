const fs=require('fs'),os=require('os'),crypto=require('crypto');
const dir='docs/2026-11-03多人压力测试';
const state=JSON.parse(fs.readFileSync(dir+'/运行状态.json','utf8'));if(state.phase!=='complete')throw Error('压力尚未完成，拒绝生成最终结论');
const plan=JSON.parse(fs.readFileSync(dir+'/环境与方案.json','utf8')),expected=JSON.parse(fs.readFileSync('docs/2026-11-03性能测试/独立预期.json','utf8'));
const phases=JSON.parse(fs.readFileSync(dir+'/阶段结果.json','utf8'));
const rows=fs.readFileSync(dir+'/请求记录.jsonl','utf8').trim().split(/\r?\n/).map(JSON.parse);
const pages=JSON.parse(fs.readFileSync(dir+'/页面抽查原始记录.json','utf8'));
const pageMeta=JSON.parse(fs.readFileSync(dir+'/浏览器抽查说明.json','utf8'));
const sort=a=>[...a].sort((x,y)=>x.categoryName.localeCompare(y.categoryName,'zh-CN'));
const pageChecks=pages.map(row=>{const r=expected.ranges.find(r=>r.start===row.start&&r.end===row.end),positive=sort(r.expected.categories).filter(x=>x.borrowCount>0);const descriptions=positive.map(x=>x.categoryName+'，'+x.borrowCount+'次，'+(x.borrowCount/r.expected.total*100).toFixed(1)+'%');
const countOK=JSON.stringify(row.top.map(x=>({bookTitle:x.bookTitle,borrowCount:x.borrowCount})))===JSON.stringify(r.expected.top)&&JSON.stringify(sort(row.categories.map(x=>({categoryName:x.categoryName,borrowCount:x.borrowCount}))))===JSON.stringify(sort(r.expected.categories));
const sectorsOK=row.sectorCount===positive.length&&JSON.stringify(row.sectorDescriptions)===JSON.stringify(descriptions)&&(positive.length?row.chartVisible&&!row.emptyChart:!row.chartVisible&&row.emptyChart);
return {sequence:row.sequence,range:r.name,ms:row.ms,countsCorrect:countOK,sectorsCorrect:sectorsOK,percentagesCorrect:row.categories.every(x=>x.percent===(r.expected.total?x.borrowCount/r.expected.total*100:0).toFixed(1)+'%'),withinLimit:row.ms<=3000,success:row.httpStatus===200&&row.businessCode===200,noErrors:row.errors.length===0,rendered:!row.loading};});
const ten=phases.find(x=>x.name==='acceptance-10'),twenty=phases.find(x=>x.name==='explore-20'),recovery=rows.filter(x=>x.phase==='recovery');
const pageOK=pageChecks.length===10&&pageChecks.every(x=>['countsCorrect','sectorsCorrect','percentagesCorrect','withinLimit','success','noErrors','rendered'].every(k=>x[k]))&&expected.ranges.every(r=>pageChecks.filter(p=>p.range===r.name).length===2)&&pageMeta.errors.length===0&&pageMeta.observedLoadPhase.phase==='acceptance-10';
const passed=ten.durationSeconds>=300&&ten.p95Ms<=2000&&ten.failures===0&&ten.incorrect===0&&pageOK&&recovery.length===5&&recovery.every(x=>x.success&&x.correct);
const samples=['资源采样.jsonl','资源采样补充.jsonl'].filter(n=>fs.existsSync(dir+'/'+n)).flatMap(n=>fs.readFileSync(dir+'/'+n,'utf8').replace(/^\uFEFF/,'').trim().split(/\r?\n/).filter(Boolean).map(x=>JSON.parse(x.replace(/^\uFEFF/,''))));
const resources=['java','mysqld'].map(name=>{const a=samples.flatMap(x=>x.processes).filter(x=>x.name===name);return {name,samples:a.length,maxCpuPercentOfMachine:Math.max(...a.map(x=>x.cpuPercentOfMachine??0)),maxWorkingSetMiB:Math.max(...a.map(x=>x.workingSetMiB)),maxPrivateMiB:Math.max(...a.map(x=>x.privateMiB))};});
const result={date:'2026-11-03',passed,thresholds:plan.thresholds,phases,pageChecks,pageMaxMs:Math.max(...pageChecks.map(x=>x.ms)),recovery,resources,environment:{cpu:os.cpus()[0].model,memoryGiB:Math.round(os.totalmem()/1024**3),logicalProcessors:os.cpus().length,browser:pages[0]?.browser,viewport:pages[0]?.viewport,buildSha256:plan.jarSha256},notes:['20人仅为摸底，不纳入正式验收判定','10名压力用户之外，浏览器抽查会额外增加查询请求','响应耗时包含fetch返回及JSON解析和正确性比对；仅有效管理员合法查询计入压力场景','每个用户完成请求后等待2—5秒，属于封闭用户模型，不代表固定RPS或容量极限']};
fs.writeFileSync(dir+'/汇总结果.json',JSON.stringify(result,null,2));
const seconds=x=>(x/1000).toFixed(3)+'秒';
const lines=['# 统计子系统多人压力测试报告','','执行日期：2026-11-03（北京时间）。10人正式验收：'+(passed?'通过':'未通过')+'。20人仅作摸底，不改变正式门槛。','',
'## 场景与预定门槛','',
'数据：独立book_db_perf_20261103，1000种图书、500名读者、10000条借阅记录。使用20个独立ADMIN测试账号，因为统计接口仅管理员可访问；这代表并行统计查询会话，不是普通读者可访问统计。用户新增仅限专用性能库，不修改默认连接配置。',
'按预定方案：5人爬升20秒→10人稳定5分钟→20人摸底2分钟。各用户轮流查询全部历史、跨12个月、单月、单日、无数据范围，每次完成后等待2—5秒，再发下次请求。20人2分钟为本轮摸底时长，不是新增验收要求。',
'正式标准：10人阶段接口P95≤2秒、成功率100%、已检查响应正确率100%；额外真实浏览器抽查10次（五种范围各2次），每次页面更新≤3秒；结束后可正常查询。','',
'## 实测结果','',
'| 阶段 | 用户数 | 持续秒 | 请求数 | 成功率 | 正确率 | P95 | 最慢 | 吞吐量 |','|---|---:|---:|---:|---:|---:|---|---|---|'];
for(const p of phases)lines.push('| '+[p.name,p.users,p.durationSeconds.toFixed(2),p.total,(100*p.success/p.total).toFixed(2)+'%',(100*(p.total-p.incorrect)/p.total).toFixed(2)+'%',seconds(p.p95Ms),seconds(p.maxMs),p.throughputPerSecond+'次/秒'].join(' | ')+' |');
lines.push('',
'P95表示95%的请求耗时不超过该值，采用最近秩法计算；不以平均数代替。错误统计同时检查HTTP状态、业务code、超时和异常，因为本项目可能HTTP200却业务失败。',
'每次响应检查分类次数、TOP10书名/次数及顺序、整体图书数/库存/在借/逾期/读者数/罚金。分类和排行来自原始夹具独立预期，罚金由数据库记录按自然日独立计算，不从被测统计响应生成答案。',
'真实浏览器10次抽查全部校验：'+(pageOK?'通过':'未通过')+'，最慢'+seconds(result.pageMaxMs)+'；计时从真实点击到数据响应、Vue更新、两帧绘制机会和遮罩消失。保存饼图扇形、百分比、图例、排行及空态的逐项比对。页面计时不等于接口耗时，也不宣称精确测量像素合成时刻。',
'压力结束后5种范围恢复检查：'+(recovery.every(x=>x.success&&x.correct)?'全部成功且结果正确':'存在异常')+'。','',
'## 环境和资源观察','',
result.environment.cpu+'，内存'+result.environment.memoryGiB+'GiB；应用、MySQL、发压程序、浏览器均在同一台电脑。生产jar和前端构建沿用已验证版本，哈希见环境与方案.json；原有用户服务和程序保持运行。浏览器'+result.environment.browser+'；视口'+JSON.stringify(result.environment.viewport)+'。','',
'资源约每5秒采样一次。前段受限采样没有读取到进程（原记录保留为空），随后提升只读采样权限补充，实际资源观测主要覆盖10人末段和20人阶段，不把空值当作零占用。CPU按整机逻辑处理器总容量归一化；MySQL数值包含本机其他库活动，不只本轮测试，列出单进程采样峰值。观测值不作为硬性验收门槛，也不以本轮观察宣称无长期内存泄漏。');
for(const r of resources)lines.push('- '+r.name+'：最高采样CPU '+r.maxCpuPercentOfMachine+'%，工作集最高'+r.maxWorkingSetMiB+'MiB，私有内存最高'+r.maxPrivateMiB+'MiB。');
lines.push('',
'## 结论的范围','',
'本轮证明这套本机环境和固定数据下，10人按约定节奏查询满足门槛；20人结果见表，仅代表已测负载。这里的10/20人是持续参与的用户数，每人有思考等待，不表示任何时刻都有10/20个在途请求；吞吐量是该节奏的观测值，不是系统最大处理能力。',
'没有执行借出、续借、归还、取书等写业务压力，也不能用本次统计查询成功替代此前写流程并发缺陷的修复验证。真人易用性按用户要求暂不执行。',
'浏览器退出确认操作曾发生自动化控制超时，重试后成功，发生在统计抽查前；不计作统计接口错误，未丢弃任何统计计时。','',
'## 保存的证据','',
'请求记录.jsonl保存每次请求时间、用户编号、范围、耗时、HTTP与业务码及正确性判定；阶段结果.json保存分阶段结果；页面抽查原始记录.json保存全部10次实际页面数据；资源采样.jsonl、截图、浏览器快照、环境与方案.json和汇总结果.json配套留存。没有保存密码或完整令牌。',
'结束时关闭本轮专用8082后端、5177代理和资源采样进程，原8080/5173服务保持。性能库和专用测试账号保留用于复测，不用于演示。');
fs.writeFileSync(dir+'/多人压力测试报告.md',lines.join('\n'));
console.log(JSON.stringify({passed,ten,twenty,pageMaxMs:result.pageMaxMs,recoveryOK:recovery.every(x=>x.success&&x.correct),resources},null,2));
