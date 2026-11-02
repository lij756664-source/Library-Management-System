<template>
  <div class="category-chart">
    <svg v-if="total" class="pie" viewBox="0 0 620 360" role="img" :aria-label="'分类借阅分布，共' + total + '次'">
      <g v-for="slice in slices" :key="slice.name">
        <path :d="slice.path" :fill="slice.color" stroke="white" stroke-width="2" tabindex="0" :aria-label="slice.name + '，' + slice.count + '次，' + slice.percent + '%'"><title>{{ slice.name }}：{{ slice.count }}次（{{ slice.percent }}%）</title></path>
        <polyline :points="slice.line" fill="none" :stroke="slice.color" stroke-width="1.5" />
        <text :x="slice.textX" :y="slice.labelY - 3" :text-anchor="slice.side" class="slice-name">{{ slice.name.length > 11 ? slice.name.slice(0, 10) + '…' : slice.name }}</text>
        <text :x="slice.textX" :y="slice.labelY + 14" :text-anchor="slice.side" class="slice-value">{{ slice.count }} 次 · {{ slice.percent }}%</text>
      </g>
    </svg>
    <el-empty v-else description="暂无借阅数据" :image-size="110" class="chart-empty" />
    <div class="chart-legend" aria-label="全部分类借阅次数">
      <div v-for="item in entries" :key="item.name" class="legend-item" :class="{ muted: !item.count }">
        <span class="legend-dot" :style="{ background: item.color }"></span><span class="legend-name" :title="item.name">{{ item.name }}</span><span class="legend-count">{{ item.count }} 次</span><span class="legend-percent">{{ total ? (item.count / total * 100).toFixed(1) : '0.0' }}%</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({ data: { type: Array, default: () => [] } })
const colors = ['#7eb8da', '#7a9e8e', '#9cabc9', '#d8b989', '#80b7b1', '#b79bba', '#91b6c0', '#b5c58e', '#d3a5a0', '#a6b4be']
const entries = computed(() => [...props.data].sort((a, b) => a.categoryName.localeCompare(b.categoryName, 'zh-CN')).map((item, i) => ({ name: item.categoryName, count: Number(item.borrowCount) || 0, color: colors[i % colors.length] })))
const total = computed(() => entries.value.reduce((sum, item) => sum + item.count, 0))
const point = (angle, radius = 112) => [310 + Math.cos(angle) * radius, 180 + Math.sin(angle) * radius]
const slices = computed(() => {
  let angle = -Math.PI / 2
  const items = entries.value.filter(item => item.count > 0).map(item => {
    const next = angle + item.count / total.value * Math.PI * 2, middle = (angle + next) / 2
    const start = point(angle), end = point(next)
    const path = item.count === total.value ? 'M 310 68 A 112 112 0 1 1 310 292 A 112 112 0 1 1 310 68 Z' : 'M 310 180 L ' + start.join(' ') + ' A 112 112 0 ' + (next - angle > Math.PI ? 1 : 0) + ' 1 ' + end.join(' ') + ' Z'
    angle = next
    return { ...item, path, middle, right: Math.cos(middle) >= 0, idealY: point(middle, 140)[1], percent: (item.count / total.value * 100).toFixed(1) }
  })
  for (const right of [false, true]) {
    const side = items.filter(item => item.right === right).sort((a, b) => a.idealY - b.idealY)
    const gap = Math.min(40, 300 / Math.max(1, side.length - 1))
    side.forEach((item, i) => { item.labelY = Math.max(26 + i * gap, Math.min(326 - (side.length - 1 - i) * gap, item.idealY)) })
    for (let i = 1; i < side.length; i++) side[i].labelY = Math.max(side[i].labelY, side[i - 1].labelY + gap)
    for (const item of side) { item.line = point(item.middle, 114).join(',') + ' ' + point(item.middle, 128).join(',') + ' ' + (right ? 458 : 162) + ',' + item.labelY; item.textX = right ? 466 : 154; item.side = right ? 'start' : 'end' }
  }
  return items
})
</script>

<style scoped>
.category-chart { min-width: 0; }
.pie { display: block; width: 100%; max-height: 380px; overflow: visible; }
.pie path { transition: filter .15s; }
.pie path:hover, .pie path:focus { filter: brightness(1.08); stroke: var(--color-text); outline: none; }
.slice-name { font-size: 14px; fill: var(--color-text); }
.slice-value { font-size: 12px; fill: #708a9e; font-variant-numeric: tabular-nums; }
.chart-legend { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px 24px; padding: 12px 4px 0; }
.legend-item { display: flex; align-items: center; gap: 8px; font-size: 12px; min-width: 0; }
.legend-dot { width: 8px; height: 8px; border-radius: 50%; flex: none; }
.legend-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.legend-count, .legend-percent { flex: none; font-variant-numeric: tabular-nums; }
.legend-percent { width: 44px; text-align: right; color: var(--color-text-secondary); }
.muted { color: var(--color-text-secondary); }
.chart-empty { min-height: 280px; }
@media (max-width: 600px) { .chart-legend { grid-template-columns: 1fr; } }
</style>
