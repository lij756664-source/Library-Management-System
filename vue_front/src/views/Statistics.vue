<template>
  <div class="statistics-page">
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" :lg="4">
        <div class="stat-card"><div class="stat-value">{{ item.value }}</div><div class="stat-label"><el-tooltip v-if="item.tip" :content="item.tip"><span>{{ item.label }} <el-icon><InfoFilled /></el-icon></span></el-tooltip><span v-else>{{ item.label }}</span></div></div>
      </el-col>
    </el-row>
    <el-card class="page-card filter-card" shadow="never">
      <el-form :inline="true" class="search-form statistics-filter" @submit.prevent="loadData">
        <el-form-item label="借出日期"><el-date-picker v-model="start" type="date" value-format="YYYY-MM-DD" placeholder="开始日" aria-label="开始日" /><span class="date-separator">至</span><el-date-picker v-model="end" type="date" value-format="YYYY-MM-DD" placeholder="结束日" aria-label="结束日" /></el-form-item>
        <el-form-item><el-button type="primary" :icon="Search" :loading="loading" @click="loadData">查询</el-button><el-button :icon="Refresh" :disabled="loading" @click="reset">全部历史</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-row :gutter="16" v-loading="loading" class="chart-row">
      <el-col :xs="24" :lg="14"><el-card class="page-card chart-card" shadow="never">
        <template #header><div class="card-heading"><span>分类借阅统计</span><el-tag type="info" size="small">{{ appliedRange }}</el-tag></div></template>
        <CategoryPie :data="categoryStats" />
      </el-card></el-col>
      <el-col :xs="24" :lg="10"><el-card class="page-card chart-card" shadow="never">
        <template #header><div class="card-heading"><span>热门图书 TOP10</span><el-tag type="info" size="small">{{ appliedRange }}</el-tag></div></template>
        <el-table :data="topBooks" stripe class="data-table" empty-text="暂无借阅数据"><el-table-column type="index" label="排名" width="60" align="center" /><el-table-column prop="bookTitle" label="书名" min-width="140" show-overflow-tooltip /><el-table-column prop="categoryName" label="分类" min-width="100" show-overflow-tooltip /><el-table-column prop="borrowCount" label="借阅次数" width="90" align="center" /></el-table>
      </el-card></el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, InfoFilled } from '@element-plus/icons-vue'
import request from '../api/request'
import CategoryPie from '../components/CategoryPie.vue'
const start = ref(''), end = ref(''), appliedRange = ref('全部历史')
const loading = ref(false), dashboard = ref({}), categoryStats = ref([]), topBooks = ref([])
const statCards = computed(() => [
  { label: '图书种类', value: dashboard.value.totalBooks ?? '—' },
  { label: '可借库存', value: dashboard.value.totalStock ?? '—' },
  { label: '在借册数', value: dashboard.value.borrowingCount ?? '—' },
  { label: '逾期册数', value: dashboard.value.overdueCount ?? '—' },
  { label: '罚金合计（元）', value: dashboard.value.totalFine == null ? '—' : Number(dashboard.value.totalFine).toFixed(2), tip: '含已归还最终罚金与未还暂计金额，不表示已收款；概况卡片不受日期筛选影响。' },
  { label: '普通用户', value: dashboard.value.userCount ?? '—' }
])
async function loadData() {
  if (!!start.value !== !!end.value || (start.value && start.value > end.value)) { ElMessage.error('请成对填写日期，开始日不得晚于结束日'); return }
  const from = start.value, to = end.value
  loading.value = true
  try {
    const res = await request.get('/statistics/overview', { params: { start: from || undefined, end: to || undefined } })
    dashboard.value = res.data.dashboard; categoryStats.value = res.data.categoryStats; topBooks.value = res.data.topBooks
    appliedRange.value = from ? from + ' 至 ' + to : '全部历史'
  } finally { loading.value = false }
}
function reset() { start.value = ''; end.value = ''; loadData() }
onMounted(loadData)
</script>

<style scoped>
.stat-card { background: var(--color-bg-card); border-radius: var(--radius); padding: 20px 16px; text-align: center; box-shadow: var(--shadow-card); margin-bottom: 16px; }
.stat-value { font-size: 26px; font-weight: 600; color: var(--color-primary-hover); font-variant-numeric: tabular-nums; }
.stat-label { margin-top: 8px; font-size: 13px; color: var(--color-text-secondary); }
.stat-label .el-icon { vertical-align: -2px; margin-left: 2px; }
.filter-card { margin-bottom: 16px; }
.statistics-filter { display: flex; align-items: center; flex-wrap: wrap; gap: 12px 20px; }
.statistics-filter :deep(.el-form-item) { margin: 0; }
.statistics-filter :deep(.el-form-item__content) { display: flex; gap: 10px; }
.statistics-filter :deep(.el-date-editor.el-input) { width: 160px; }
.date-separator { color: var(--color-text-secondary); }
.card-heading { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 10px; }
.chart-card { margin-bottom: 16px; }
@media (max-width: 600px) { .statistics-filter :deep(.el-form-item__content) { flex-wrap: wrap; } }
</style>
