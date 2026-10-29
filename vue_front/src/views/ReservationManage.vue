<template>
  <el-card class="page-card" shadow="never">
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="用户/书名" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="排队中" value="WAITING" />
          <el-option label="待取书" value="READY" />
          <el-option label="已取书" value="FULFILLED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="已过期" value="EXPIRED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadData">搜索</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" stripe class="data-table">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="nickname" label="预约人" width="110" />
      <el-table-column prop="bookTitle" label="书名" min-width="150" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <ReservationStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="队列" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.status === 'WAITING' || row.status === 'READY'">
            {{ row.queueNo }}/{{ row.queueSize }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="预约时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="取书截止" width="170">
        <template #default="{ row }">{{ formatDateTime(row.expireTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'WAITING' || row.status === 'READY'"
            type="danger"
            link
            @click="handleCancel(row)"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getAllReservations, cancelReservation } from '../api/reservation'
import ReservationStatusTag from '../components/ReservationStatusTag.vue'
import { formatDateTime } from '../utils/date'

const loading = ref(false)
const records = ref([])
const searchForm = reactive({ keyword: '', status: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAllReservations({
      keyword: searchForm.keyword || undefined,
      status: searchForm.status || undefined
    })
    records.value = res.data
  } finally {
    loading.value = false
  }
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.status = ''
  loadData()
}

const handleCancel = (row) => {
  ElMessageBox.confirm(`确定取消 ${row.nickname} 对《${row.bookTitle}》的预约吗？队列将自动顺延。`, '取消预约', { type: 'warning' })
    .then(async () => {
      await cancelReservation(row.id)
      ElMessage.success('已取消，队列已更新')
      loadData()
    })
    .catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
</style>
