const fs = require('fs');
const http = require('http');
const path = require('path');
const root = path.resolve('vue_front/dist');
const records = [];
const output = 'docs/新版饼图性能原始记录.json';
if (fs.existsSync(output)) throw new Error('New pie chart performance result already exists; archive it before a new run.');
http.createServer((req, res) => {
  if (req.url === '/__qa/perf.js') {
    res.setHeader('Content-Type', 'application/javascript; charset=utf-8');
    res.end(fs.readFileSync('scripts/qa-pie-perf-probe.js'));
    return;
  }
  if (req.url === '/__qa/metric' && req.method === 'POST') {
    let data = '';
    req.on('data', chunk => { data += chunk; if (data.length > 256000) req.destroy(); });
    req.on('end', () => {
      try { records.push(JSON.parse(data)); fs.writeFileSync(output, JSON.stringify(records, null, 2)); res.end('ok'); }
      catch { res.statusCode = 400; res.end('invalid metric'); }
    });
    return;
  }
  if (req.url.startsWith('/api/')) {
    const upstream = http.request({ hostname: '127.0.0.1', port: 8081, path: req.url, method: req.method,
      headers: { ...req.headers, host: '127.0.0.1:8081' } }, response => {
      res.writeHead(response.statusCode, response.headers); response.pipe(res);
    });
    upstream.on('error', () => { res.statusCode = 502; res.end('Backend unavailable'); });
    req.pipe(upstream); return;
  }
  let filename = path.resolve(root, '.' + decodeURIComponent(req.url.split('?')[0]));
  if (!filename.startsWith(root + path.sep) && filename !== root) { res.statusCode = 403; res.end(); return; }
  if (!fs.existsSync(filename) || fs.statSync(filename).isDirectory()) filename = path.join(root, 'index.html');
  const ext = path.extname(filename);
  res.setHeader('Content-Type', ({ '.html': 'text/html; charset=utf-8', '.js': 'application/javascript', '.css': 'text/css' })[ext] || 'application/octet-stream');
  if (ext === '.html') res.end(fs.readFileSync(filename, 'utf8').replace('<head>', '<head><script src="/__qa/perf.js"></script>'));
  else fs.createReadStream(filename).pipe(res);
}).listen(5175, '127.0.0.1', () => console.log('Pie performance harness 5175 -> 8081'));
