<template>
  <el-card class="page-card" shadow="never">
    <el-form :inline="true" class="search-form">
      <el-form-item label="状态">
        <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px" @change="loadData">
          <el-option label="排队中" value="WAITING" />
          <el-option label="待取书" value="READY" />
          <el-option label="已取书" value="FULFILLED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已过期" value="EXPIRED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      title="无库存图书可预约排队。有人归还后，队首将进入「待取书」，请在期限内取书，过期自动顺延下一位。"
      type="info"
      show-icon
      :closable="false"
      class="rules-alert"
    />

    <el-table :data="records" v-loading="loading" stripe class="data-table">
      <el-table-column prop="bookTitle" label="书名" min-width="160" />
      <el-table-column prop="bookAuthor" label="作者" width="120" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <ReservationStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="排队位置" width="110" align="center">
        <template #default="{ row }">
          <span v-if="row.status === 'WAITING' || row.status === 'READY'">
            {{ row.queueNo }} / {{ row.queueSize }}
          </span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="预约时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="取书截止" width="170">
        <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'READY'" type="primary" link @click="handlePickup(row)">取书</el-button>
          <el-button
            v-if="row.status === 'WAITING' || row.status === 'READY'"
            type="danger"
            link
            @click="handleCancel(row)"
          >取消</el-button>
          <span v-if="row.status !== 'WAITING' && row.status !== 'READY'" class="text-muted">-</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getMyReservations, cancelReservation, pickupReservation } from '../api/reservation'
import ReservationStatusTag from '../components/ReservationStatusTag.vue'
import { formatDateTime } from '../utils/date'

const loading = ref(false)
const records = ref([])
const status = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyReservations({ status: status.value || undefined })
    records.value = res.data
  } finally {
    loading.value = false
  }
}

const handlePickup = (row) => {
  ElMessageBox.confirm(`确定领取《${row.bookTitle}》并转为借阅吗？`, '取书确认', { type: 'info' })
    .then(async () => {
      await pickupReservation(row.id)
      ElMessage.success('取书成功，已加入我的借阅')
      loadData()
    })
    .catch(() => {})
}

const handleCancel = (row) => {
  ElMessageBox.confirm(`确定取消《${row.bookTitle}》的预约吗？取消后队列将自动顺延。`, '取消预约', { type: 'warning' })
    .then(async () => {
      await cancelReservation(row.id)
      ElMessage.success('已取消预约')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.rules-alert { margin-bottom: 16px; }
.text-muted { color: var(--color-text-muted); }
</style>
