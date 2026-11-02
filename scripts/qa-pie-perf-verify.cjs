const fs = require('fs');
const os = require('os');
const crypto = require('crypto');
const { execFileSync } = require('child_process');
const expected = JSON.parse(fs.readFileSync('docs/性能测试预期.json', 'utf8'));
const records = JSON.parse(fs.readFileSync('docs/新版饼图性能原始记录.json', 'utf8'));
const sort = items => [...items].sort((a, b) => a.categoryName.localeCompare(b.categoryName, 'zh-CN'));
const groups = expected.ranges.map(range => {
  const selected = records.filter(row => row.start === range.start && row.end === range.end);
  const expectedCategories = sort(range.expected.categories);
  const positive = expectedCategories.filter(item => item.borrowCount > 0);
  const descriptions = positive.map(item => item.categoryName + '，' + item.borrowCount + '次，' + (item.borrowCount / range.expected.total * 100).toFixed(1) + '%');
  const checks = selected.map(row => {
    const top = row.top.map(item => ({ bookTitle: item.bookTitle, borrowCount: item.borrowCount }));
    const categories = sort(row.categories.map(item => ({ categoryName: item.categoryName, borrowCount: item.borrowCount })));
    const countsCorrect = JSON.stringify(top) === JSON.stringify(range.expected.top) && JSON.stringify(categories) === JSON.stringify(expectedCategories);
    const percentagesCorrect = row.categories.every(item => item.percent === (range.expected.total ? item.borrowCount / range.expected.total * 100 : 0).toFixed(1) + '%');
    const sectorsCorrect = row.sectorCount === positive.length && JSON.stringify(row.sectorDescriptions) === JSON.stringify(descriptions)
      && (positive.length ? row.chartVisible && !row.emptyChart : !row.chartVisible && row.emptyChart);
    return { sequence: row.sequence, kind: row.kind, ms: row.ms, countsCorrect, percentagesCorrect, sectorsCorrect,
      ranksCorrect: row.top.every((item, index) => item.rank === index + 1), httpOk: row.httpStatus === 200 && row.businessCode === 200,
      noErrors: row.errors.length === 0, rendered: !row.loading, withinLimit: row.ms <= 3000 };
  });
  return { name: range.name, start: range.start, end: range.end, checks,
    maxMs: Math.max(...checks.map(row => row.ms)),
    passed: checks.length === 5 && checks.every(row => ['countsCorrect','percentagesCorrect','sectorsCorrect','ranksCorrect','httpOk','noErrors','rendered','withinLimit'].every(key => row[key])) };
});
const git = (...args) => execFileSync('git', ['-c','safe.directory=C:/图书管理系统/Library-Management-System',...args], { encoding:'utf8' }).trim();
const result = { testedAt: new Date().toISOString(), commit: git('rev-parse','HEAD'), firstQueryRetained: records[0]?.kind === 'first-navigation',
  totalQueries: records.length, scale: expected.scale,
  environment: { cpu: os.cpus()[0].model, memoryGiB: Math.round(os.totalmem()/1024**3), os: os.type()+' '+os.release(), node: process.version,
    java: '17.0.20.1', springBoot: '3.2.5', mysql: '8.0.43', browser: records[0]?.browser, viewport: records[0]?.viewport,
    deployment: 'Same computer; production static assets + test HTTP proxy 5175 -> Spring Boot 8081 -> book_db_perf',
    distIndexSha256: crypto.createHash('sha256').update(fs.readFileSync('vue_front/dist/index.html')).digest('hex') },
  groups, passed: records.length === 25 && records[0]?.kind === 'first-navigation' && groups.every(group => group.passed) };
fs.writeFileSync('docs/新版饼图性能结果.json', JSON.stringify(result, null, 2));
const lines = [ '# 新版饼图性能复测结果', '', '执行日期：2026-11-02。被测版本：'+result.commit.slice(0,7)+'。结论：'+(result.passed?'本机本轮25次查询通过。':'存在未通过项，见原始记录。'), '',
  '测试使用book_db_perf：1000种图书、3000册（2000可借+1000在借）、10类、500名读者、10000条借阅，其中1000未还含200逾期；历史跨至少12个月。没有覆盖或改写book_db_current。', '',
  '| 查询范围 | 第1次ms | 第2次ms | 第3次ms | 第4次ms | 第5次ms | 最大ms | 结果 |', '|---|---:|---:|---:|---:|---:|---:|---|' ];
for (const group of groups) lines.push('| '+[group.name,...group.checks.map(row=>row.ms.toFixed(1)),group.maxMs.toFixed(1),group.passed?'通过':'未通过'].join(' | ')+' |');
lines.push('', '每次均检查：HTTP与业务code成功、全部分类次数和百分比正确、TOP10顺序和次数正确、正值分类对应扇形与文字标注、零数据空态、无页面错误、加载遮罩消失。独立期望来自原始夹具生成时按借阅记录计算的JSON，不由被测统计SQL反算。', '',
  '计时从真实按钮点击或首次统计菜单点击开始，到请求完成、Vue更新、至少两帧绘制机会且遮罩不可见，再读取SVG、图例和表格。首次菜单进入计入全部历史第1次，没有先查询预热。第一轮前发生测试服务器连接超时，已修复服务器访问方式，未执行统计请求；启动后只加载图书管理页作为已登录前置，没有统计预热。', '',
  '环境：'+result.environment.cpu+'；内存'+result.environment.memoryGiB+'GiB；'+result.environment.os+'；Java '+result.environment.java+'；MySQL '+result.environment.mysql+'；'+result.environment.browser+'；视口1440×960。前端为生产构建，应用与数据库均在本机。除原有服务外未主动运行并发压力任务；不擅自关闭用户其他程序。', '',
  '本轮只证明当前机器、当前数据集、单人操作下满足每次≤3秒，不承诺多人或其他机器相同性能。原始记录、每项比对、版本与构建哈希均保留。首次登录图书页加载不是统计查询计时的一部分。', '',
  '证据：新版饼图性能原始记录.json、新版饼图性能结果.json、新版饼图性能历史截图.png、新版饼图性能空数据截图.jpg。脚本qa-pie-perf-probe.js仅用于测试观察，不修改应用实现。', '',
  '剩余工作：线下3人易用性、与组员联调，以及测试进度中仍标为未执行的精确场景；本报告不是整个系统全部验收通过声明。', '' );
fs.writeFileSync('docs/新版饼图性能复测报告.md', lines.join('\n'));
console.log(JSON.stringify({ passed: result.passed, total: result.totalQueries, firstQueryRetained:result.firstQueryRetained, groups:groups.map(group=>({name:group.name,maxMs:group.maxMs,passed:group.passed})) },null,2));
if (!result.passed) process.exitCode = 1;
