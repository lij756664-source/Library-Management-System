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
      <el-table-column label="应还日期（北京）" width="170">
        <template #default="{ row }">{{ String(row.dueTime).slice(0,10) }}</template>
      </el-table-column>
      <el-table-column label="剩余/逾期" width="120" align="center">
        <template #default="{ row }">
          <span v-if="row.status === 'BORROWING'" :class="{ 'text-danger': row.overdue }">{{ getRemainText(row.remainDays, row.overdue) }}</span>
          <span v-else class="text-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="renewCount" label="续借次数" width="90" align="center" />
      <el-table-column label="罚金(元)" width="100" align="right">
        <template #default="{ row }">
          <span :class="{ 'text-danger': row.overdue }">
            {{ formatFine(row) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="overdueDays" label="逾期天数" width="100" />
      <el-table-column label="日费(元)" width="100"><template #default="{ row }">{{ Number(row.finePerDaySnapshot).toFixed(2) }}</template></el-table-column>
      <el-table-column label="金额性质" width="100"><template #default="{ row }"><el-tooltip :content="row.status === 'RETURNED' ? '按实际归还日期核算，不表示已缴费' : '按北京时间当天暂计，归还时核算最终金额'"><el-tag :type="row.status === 'RETURNED' ? 'success' : 'info'" size="small">{{ row.status === 'RETURNED' ? '最终' : '暂计' }}</el-tag></el-tooltip></template></el-table-column>
      <el-table-column label="实际归还时间" width="170"><template #default="{ row }">{{ row.returnTime ? formatDateTime(row.returnTime) : '尚未归还' }}</template></el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <!-- 仅借阅中可操作：逾期禁止续借，归还始终允许 -->
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
import { getMyBorrows, renewBook, returnBook } from '../api/borrow'
import BorrowStatusTag from '../components/BorrowStatusTag.vue'
import { formatDateTime, getRemainText } from '../utils/date'
import { calcDisplayFine } from '../utils/fine'

const loading = ref(false)
const records = ref([])
const status = ref('')
const finePerDay = ref(0.5)

const formatFine = (row) => calcDisplayFine(row, finePerDay.value).toFixed(2)

/** 借阅记录查询：GET /api/borrows/my，可按借阅中/已归还筛选 */
const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyBorrows({ status: status.value || undefined })
    records.value = res.data
  } finally {
    loading.value = false
  }
}

/** 续借：PUT /api/borrows/{id}/renew，逾期按钮已禁用 */
const handleRenew = (row) => {
  ElMessageBox.confirm(`确定续借《${row.bookTitle}》吗？`, '续借确认', { type: 'info' })
    .then(async () => {
      await renewBook(row.id)
      ElMessage.success('续借成功')
      loadData()
    })
    .catch(() => {})
}

/** 归还：PUT /api/borrows/{id}/return；逾期时提示将核算罚金 */
const handleReturn = (row) => {
  const tip = row.overdue ? `该书已逾期 ${row.overdueDays} 天，暂计罚金 ${formatFine(row)} 元，归还时将核算最终金额。确定归还吗？` : `确定归还《${row.bookTitle}》吗？`
  ElMessageBox.confirm(tip, '归还确认', { type: row.overdue ? 'warning' : 'info' })
    .then(async () => {
      await returnBook(row.id)
      ElMessage.success('归还成功')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)

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
