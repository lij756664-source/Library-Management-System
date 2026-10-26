<template>
  <el-card class="page-card" shadow="never">
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="用户/书名" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="借阅中" value="BORROWING" />
          <el-option label="已归还" value="RETURNED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" stripe class="data-table">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="nickname" label="借阅人" width="110" />
      <el-table-column prop="bookTitle" label="书名" min-width="150" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <BorrowStatusTag :status="row.status" :overdue="row.overdue" />
        </template>
      </el-table-column>
      <el-table-column label="借阅时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.borrowTime) }}</template>
      </el-table-column>
      <el-table-column label="应还时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.dueTime) }}</template>
      </el-table-column>
      <el-table-column label="剩余/逾期" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.status === 'BORROWING'" :class="{ 'text-danger': row.overdue }">
            {{ getRemainText(row.remainDays, row.overdue) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="renewCount" label="续借" width="70" align="center" />
      <el-table-column label="罚款(元)" width="100" align="right">
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.overdue }">
            {{ calcDisplayFine(row, finePerDay).toFixed(2) }}
          </span>
          <span v-if="isEstimatedFine(row)" class="fine-hint">预估</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'BORROWING'" type="success" link @click="handleReturn(row)">代还</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getAllBorrows, returnBook, getBorrowRules } from '../api/borrow'
import BorrowStatusTag from '../components/BorrowStatusTag.vue'
import { formatDateTime, getRemainText } from '../utils/date'
import { calcDisplayFine, isEstimatedFine } from '../utils/fine'

const loading = ref(false)
const records = ref([])
const searchForm = reactive({ keyword: '', status: '' })
const finePerDay = ref(0.5)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAllBorrows({
      keyword: searchForm.keyword || undefined,
      status: searchForm.status || undefined
    })
    records.value = res.data
  } finally { loading.value = false }
}

const resetSearch = () => { searchForm.keyword = ''; searchForm.status = ''; loadData() }

const handleReturn = (row) => {
  ElMessageBox.confirm(`确定为 ${row.nickname} 代还《${row.bookTitle}》吗？`, '代还确认', { type: 'warning' })
    .then(async () => { await returnBook(row.id); ElMessage.success('归还成功'); loadData() })
    .catch(() => {})
}

onMounted(async () => {
  try {
    const rulesRes = await getBorrowRules()
    if (rulesRes.data?.fine_per_day) {
      finePerDay.value = Number(rulesRes.data.fine_per_day)
    }
  } catch (_) { /* 使用默认 0.5 */ }
  loadData()
})
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.text-danger { color: var(--color-danger-soft); font-weight: 500; }
.fine-hint { font-size: 11px; color: var(--color-text-muted); margin-left: 2px; }
</style>
