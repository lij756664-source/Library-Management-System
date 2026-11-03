const fs=require('fs'),vm=require('vm');
const dir='docs/2026-11-03性能测试';
let script=fs.readFileSync('scripts/qa-pie-perf-verify.cjs','utf8').split('const lines =')[0]
 .replace('docs/性能测试预期.json',dir+'/独立预期.json')
 .replace('docs/新版饼图性能原始记录.json',dir+'/页面原始记录.json')
 .replace('docs/新版饼图性能结果.json',dir+'/逐项校验结果.json')
 .replaceAll('book_db_perf','book_db_perf_20261103').replaceAll('5175','5177').replaceAll('8081','8082');
script+=`
const gitDiff=git('diff');
result.workingTreeDiffSha256=crypto.createHash('sha256').update(gitDiff).digest('hex');
result.productionJarSha256=crypto.createHash('sha256').update(fs.readFileSync('springboot_back/target/book-management-1.0.0.jar')).digest('hex');
result.distAssets=Object.fromEntries(fs.readdirSync('vue_front/dist/assets').map(name=>[name,crypto.createHash('sha256').update(fs.readFileSync('vue_front/dist/assets/'+name)).digest('hex')]));
fs.writeFileSync('${dir}/逐项校验结果.json',JSON.stringify(result,null,2));
const lines=['# 统计子系统性能测试报告','','日期：2026-11-03（北京时间）。结论：'+(result.passed?'本机本轮25次页面查询全部通过。':'存在未通过项，见逐项校验结果。'),'',
'## 测试数据和版本','',
'独立库book_db_perf_20261103：1000种图书、3000册（2000可借+1000在借）、10类、500读者、10000条借阅，其中1000未还含200逾期；历史跨至少12个月。新建夹具，无预约占用；未改写book_db_current或book_db_regression，也未改本地默认连接配置。',
'使用本轮含日期错误提示修复的代码，HEAD '+result.commit+' 另有未提交改动；后端已重新package、前端已重新build。版本信息及构建SHA256见逐项校验结果.json。性能库使用初始化脚本原有索引，没有沿用旧性能准备脚本额外添加的借出时间复合索引。','',
'## 操作与结果','',
'生产静态页面5177 → 专用后端8082 → 独立性能库。首次进入图书管理页完成登录前置，然后真实点击数据统计；首次统计计入全部历史第1次，不预热统计。其余用例真实点击查询按钮。',
'从点击开始，经接口响应、Vue更新、至少两帧绘制机会，直到加载遮罩不可见，再读取实际SVG、图例和排行。计时为测试观察口径，不宣称精确测量浏览器像素合成完成时刻；页面探针通过独立代理加载，没有修改应用源代码。','',
'| 范围 | 第1次ms | 第2次ms | 第3次ms | 第4次ms | 第5次ms | 最大ms | 结果 |','|---|---:|---:|---:|---:|---:|---:|---|'];
for(const group of groups)lines.push('| '+[group.name,...group.checks.map(x=>x.ms.toFixed(1)),group.maxMs.toFixed(1),group.passed?'通过':'未通过'].join(' | ')+' |');
lines.push('',
'每次分别与夹具生成时根据原始借阅记录独立计算的预期核对：分类次数、百分比、TOP10顺序和次数、排名编号、正值扇形数量和文字、无数据空态；同时检查HTTP和业务code成功、加载结束、页面无错误。25次逐项结果均保留，不能只看平均值。','',
'## 环境和适用范围','',
result.environment.cpu+'；内存'+result.environment.memoryGiB+'GiB；'+result.environment.os+'；Java '+result.environment.java+'；MySQL '+result.environment.mysql+'；'+result.environment.browser+'；1440×960视口。应用与数据库在本机，未主动运行并发压力；原有服务和用户其他程序保留。','',
'通过标准沿用测试计划：上述数据规模下，5种范围各5次，每次页面查询≤3000ms且内容正确。该结果证明当前机器、数据集、单人顺序查询满足目标，不证明多人压力、其他电脑、无限数据规模或全系统所有页面性能。首次计时包含第一次打开统计页，但不包含登录或整个网站首次下载时间。','',
'自动化等待第二条记录时，选择器匹配曾超时；记录已实际生成，核对sequence=2后继续，没有丢弃或重测该次来替换较慢数据。所有sequence=1—25完整保留。','',
'## 证据与结束状态','',
'独立预期.json、页面原始记录.json、逐项校验结果.json、环境核对.json、全部历史截图.png、无数据截图.png和测试脚本。截图仅为代表页面，25次全部值和检查见JSON。浏览器错误日志未见error。',
'性能库保留供复测；测试结束关闭本轮专用后端与计时代理，不影响用户原8080/5173服务。真人易用性按用户要求暂不执行。');
fs.writeFileSync('${dir}/性能测试报告.md',lines.join('\\n'));
console.log(JSON.stringify({passed:result.passed,total:result.totalQueries,groups:groups.map(g=>({name:g.name,times:g.checks.map(x=>x.ms),max:g.maxMs,passed:g.passed}))},null,2));
if(!result.passed)process.exitCode=1;
`;
vm.runInNewContext(script,{require,console,process,Buffer},{filename:__filename});
