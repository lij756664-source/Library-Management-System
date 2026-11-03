const fs=require('fs'),cp=require('child_process');const {credential}=require('./qa-config.cjs');const dir=process.argv.includes('--load')?'docs/2026-11-03多人压力测试':'docs/2026-11-03性能测试';fs.mkdirSync(dir,{recursive:true});
const output=dir+(process.argv.includes('--load')?'/页面抽查原始记录.json':'/页面原始记录.json');
if(fs.existsSync(output))throw Error('证据已存在，请先选择新的证据目录');
fs.mkdirSync('tmp',{recursive:true});
let proxy=fs.readFileSync('scripts/qa-pie-perf-server.cjs','utf8').replace('docs/新版饼图性能原始记录.json',output).replaceAll('8081','8082').replaceAll('5175','5177');
fs.writeFileSync('tmp/load-proxy.cjs',proxy);
const children=[];function launch(exe,args,name,env=process.env){const fd=fs.openSync('tmp/'+name+'.log','a');const c=cp.spawn(exe,args,{cwd:process.cwd(),env,windowsHide:true,detached:true,stdio:['ignore',fd,fd]});c.unref();fs.closeSync(fd);children.push({name,pid:c.pid});}
launch('C:/Users/syq02/.jdks/temurin-17.0.20.1/bin/java.exe',['-jar','springboot_back/target/book-management-1.0.0.jar','--spring.profiles.active=local','--server.port=8082','--spring.datasource.url=jdbc:mysql://localhost:3306/book_db_perf_20261103?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'],'load-backend',{...process.env,DB_USERNAME:credential('username'),DB_PASSWORD:credential('password')});
launch(process.execPath,['tmp/load-proxy.cjs'],'load-proxy');fs.writeFileSync('tmp/load-processes.json',JSON.stringify(children));console.log(JSON.stringify(children));
