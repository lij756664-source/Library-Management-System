<template>
  <el-card class="page-card" shadow="never">
    <el-form :inline="true" class="search-form">
      <el-form-item label="状态">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
          <el-option label="借阅中" value="BORROWING" />
          <el-option label="已归还" value="RETURNED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" stripe class="data-table">
      <el-table-column prop="bookTitle" label="书名" min-width="160" />
      <el-table-column prop="bookAuthor" label="作者" width="120" />
      <el-table-column prop="categoryName" label="分类" width="100" align="center" />
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
          <span :class="{ 'text-danger': row.overdue }">{{ getRemainText(row.remainDays, row.overdue) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="renewCount" label="续借次数" width="90" align="center" />
      <el-table-column label="罚款(元)" width="100" align="right">
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.overdue }">
            {{ formatFine(row) }}
          </span>
          <span v-if="isEstimatedFine(row)" class="fine-hint">预估</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'BORROWING'">
            <el-button type="warning" link :disabled="row.overdue" @click="handleRenew(row)">续借</el-button>
            <el-button type="success" link @click="handleReturn(row)">归还</el-button>
          </template>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getMyBorrows, renewBook, returnBook, getBorrowRules } from '../api/borrow'
import BorrowStatusTag from '../components/BorrowStatusTag.vue'
import { formatDateTime, getRemainText } from '../utils/date'
import { calcDisplayFine, isEstimatedFine } from '../utils/fine'

const loading = ref(false)
const records = ref([])
const status = ref('')
const finePerDay = ref(0.5)

const formatFine = (row) => calcDisplayFine(row, finePerDay.value).toFixed(2)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyBorrows({ status: status.value || undefined })
    records.value = res.data
  } finally {
    loading.value = false
  }
}

const handleRenew = (row) => {
  ElMessageBox.confirm(`确定续借《${row.bookTitle}》吗？`, '续借确认', { type: 'info' })
    .then(async () => {
      await renewBook(row.id)
      ElMessage.success('续借成功')
      loadData()
    })
    .catch(() => {})
}

const handleReturn = (row) => {
  const tip = row.overdue ? '该书已逾期，归还将产生罚款，确定归还吗？' : `确定归还《${row.bookTitle}》吗？`
  ElMessageBox.confirm(tip, '归还确认', { type: row.overdue ? 'warning' : 'info' })
    .then(async () => {
      await returnBook(row.id)
      ElMessage.success('归还成功')
      loadData()
    })
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
.text-muted { color: var(--color-text-muted); }
.fine-hint {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-left: 2px;
}
</style>
