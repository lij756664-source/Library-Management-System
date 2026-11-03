// Build a fresh isolated fixture without overwriting historical evidence or adding tuning indexes.
const fs=require('fs'),vm=require('vm'),path=require('path');
const dir='docs/2026-11-03性能测试';fs.mkdirSync(dir,{recursive:true});
fs.mkdirSync('tmp',{recursive:true});
let script=fs.readFileSync('scripts/qa-perf-data.cjs','utf8')
 .replaceAll('book_db_perf','book_db_perf_20261103')
 .replace('docs/性能测试预期.json',dir+'/独立预期.json')
 .replace(' CREATE INDEX idx_borrow_time_book ON borrow_record(borrow_time,book_id);','')
 .replaceAll('2026-11-01','2026-11-03');
vm.runInNewContext(script,{require,console,process,Buffer,__dirname:path.resolve('scripts')},{filename:__filename});
let server=fs.readFileSync('scripts/qa-pie-perf-server.cjs','utf8')
 .replace('docs/新版饼图性能原始记录.json',dir+'/页面原始记录.json')
 .replaceAll('8081','8082').replaceAll('5175','5177');
fs.writeFileSync('tmp/perf-current-server.cjs',server);
console.log('Fresh performance fixture and observer server prepared.');
