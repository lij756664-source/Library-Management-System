/* Test-only observer: starts at a real click, reads rendered SVG/legend/table, never changes application data. */
(() => {
  let pendingStart = null;
  let pendingKind = null;
  let sequence = 0;
  document.addEventListener('click', event => {
    const button = event.target.closest('button');
    const menu = event.target.closest('[role="menuitem"]');
    if (button?.textContent.trim() === '查询' || menu?.textContent.trim() === '数据统计') {
      pendingStart = performance.now();
      pendingKind = button ? 'query-click' : 'first-navigation';
    }
  }, true);
  const open = XMLHttpRequest.prototype.open;
  const send = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(method, url) {
    this.qaUrl = String(url);
    return open.apply(this, arguments);
  };
  XMLHttpRequest.prototype.send = function() {
    if (this.qaUrl.includes('/statistics/overview')) {
      const began = pendingStart ?? performance.now();
      const kind = pendingKind ?? 'automatic';
      const requestUrl = new URL(this.qaUrl, location.origin);
      pendingStart = pendingKind = null;
      this.addEventListener('loadend', () => {
        const xhr = this;
        requestAnimationFrame(() => requestAnimationFrame(function finish() {
          const masks = [...document.querySelectorAll('.statistics-page .el-loading-mask')];
          const loading = masks.some(mask => getComputedStyle(mask).display !== 'none' && Number(getComputedStyle(mask).opacity) > 0);
          if (loading && performance.now() - began < 15000) { requestAnimationFrame(finish); return; }
          const categories = [...document.querySelectorAll('.legend-item')].map(row => ({
            categoryName: row.querySelector('.legend-name').textContent.trim(),
            borrowCount: parseInt(row.querySelector('.legend-count').textContent, 10),
            percent: row.querySelector('.legend-percent').textContent.trim()
          }));
          const top = [...document.querySelectorAll('.chart-row .el-table__body tr')].map(row => {
            const cells = [...row.querySelectorAll('td')].map(cell => cell.textContent.trim());
            return { rank: Number(cells[0]), bookTitle: cells[1], categoryName: cells[2], borrowCount: Number(cells[3]) };
          });
          const chart = document.querySelector('svg.pie');
          const box = chart?.getBoundingClientRect();
          let businessCode = null;
          try { businessCode = JSON.parse(xhr.responseText).code; } catch {}
          const result = {
            sequence: ++sequence, kind,
            start: requestUrl.searchParams.get('start') || '', end: requestUrl.searchParams.get('end') || '',
            ms: Math.round((performance.now() - began) * 100) / 100,
            httpStatus: xhr.status, businessCode, loading, categories, top,
            sectorCount: document.querySelectorAll('.pie path').length,
            sectorDescriptions: [...document.querySelectorAll('.pie path')].map(path => path.getAttribute('aria-label')),
            chartVisible: !!box && box.width > 0 && box.height > 0,
            emptyChart: !!document.querySelector('.category-chart .el-empty'),
            errors: [...document.querySelectorAll('.el-message--error')].map(message => message.textContent.trim()),
            browser: navigator.userAgent, viewport: { width: innerWidth, height: innerHeight }
          };
          let output = document.getElementById('qa-pie-measurement');
          if (!output) { output = document.createElement('pre'); output.id = 'qa-pie-measurement'; document.body.appendChild(output); }
          output.textContent = 'QA饼图测量 ' + JSON.stringify(result);
          navigator.sendBeacon('/__qa/metric', new Blob([JSON.stringify(result)], { type: 'application/json' }));
        }));
      }, { once: true });
    }
    return send.apply(this, arguments);
  };
})();
