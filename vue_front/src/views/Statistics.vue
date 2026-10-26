<template>
  <div v-loading="loading" class="statistics-page">
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="8" :md="4" v-for="item in statCards" :key="item.label">
        <div class="stat-card">
          <div class="stat-value">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="chart-row">
      <el-col :span="12">
        <el-card class="page-card" shadow="never">
          <template #header><span>分类借阅统计</span></template>
          <el-table :data="categoryStats" stripe class="data-table" size="small">
            <el-table-column prop="categoryName" label="分类" />
            <el-table-column prop="borrowCount" label="借阅总量" align="center" />
            <el-table-column prop="overdueCount" label="当前逾期" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.overdueCount > 0" type="danger" size="small">{{ row.overdueCount }}</el-tag>
                <span v-else class="text-muted">0</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="page-card" shadow="never">
          <template #header><span>热门图书 TOP5</span></template>
          <el-table :data="topBooks" stripe class="data-table" size="small">
            <el-table-column prop="bookTitle" label="书名" show-overflow-tooltip />
            <el-table-column prop="categoryName" label="分类" width="100" />
            <el-table-column prop="borrowCount" label="借阅次数" width="100" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getOverview } from '../api/statistics'
import { getAllBorrows, getBorrowRules } from '../api/borrow'
import { calcDisplayFine } from '../utils/fine'

const loading = ref(false)
const dashboard = ref({})
const categoryStats = ref([])
const topBooks = ref([])
const finePerDay = ref(0.5)

const statCards = computed(() => [
  { label: '图书种类', value: dashboard.value.totalBooks ?? 0 },
  { label: '可借库存', value: dashboard.value.totalStock ?? 0 },
  { label: '在借册数', value: dashboard.value.borrowingCount ?? 0 },
  { label: '逾期册数', value: dashboard.value.overdueCount ?? 0 },
  { label: '累计罚款(元)', value: Number(dashboard.value.totalFine ?? 0).toFixed(2) },
  { label: '普通用户', value: dashboard.value.userCount ?? 0 }
])

const loadData = async () => {
  loading.value = true
  try {
    const [overviewRes, rulesRes, borrowsRes] = await Promise.all([
      getOverview(),
      getBorrowRules(),
      getAllBorrows({})
    ])
    if (rulesRes.data?.fine_per_day) {
      finePerDay.value = Number(rulesRes.data.fine_per_day)
    }
    dashboard.value = overviewRes.data.dashboard
    categoryStats.value = overviewRes.data.categoryStats
    topBooks.value = overviewRes.data.topBooks

    // 累计罚款兜底：含已归还实际罚款 + 逾期未还预估罚款
    const fineSum = (borrowsRes.data || []).reduce(
      (sum, row) => sum + calcDisplayFine(row, finePerDay.value),
      0
    )
    if (fineSum > 0) {
      dashboard.value = { ...dashboard.value, totalFine: fineSum }
    }
  } finally { loading.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 20px 16px;
  text-align: center;
  box-shadow: var(--shadow-card);
  margin-bottom: 16px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(100, 120, 140, 0.1);
}

.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: var(--color-primary-hover);
}

.stat-label {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 400;
  color: var(--color-text-muted);
}

.chart-row {
  margin-top: 0;
}

.text-muted {
  color: var(--color-text-muted);
}
</style>
