<template>
  <el-card class="page-card" shadow="never">
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="操作人">
        <el-input v-model="searchForm.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-select v-model="searchForm.operation" placeholder="全部" clearable filterable style="width: 170px">
          <el-option v-for="op in operationOptions" :key="op" :label="op" :value="op" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="logs" v-loading="loading" stripe class="data-table">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="操作人" width="120">
        <template #default="{ row }">{{ row.username || '未登录' }}</template>
      </el-table-column>
      <el-table-column label="操作类型" width="140" align="center">
        <template #default="{ row }">
          <el-tag :type="tagType(row.operation)" size="small">{{ row.operation }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="操作详情" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">{{ row.detail || '-' }}</template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="150">
        <template #default="{ row }">{{ row.ip || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getLogs } from '../api/log'
import { formatDateTime } from '../utils/date'

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const searchForm = reactive({ username: '', operation: '' })

// 与后端写入日志时使用的操作类型字面值保持一致
const operationOptions = [
  '注册', '登录', '修改个人信息', '修改密码', '更新头像',
  '启用账号', '停用账号', '修改账号信息', '重置密码',
  '图书入库', '修改图书信息', '维护馆藏副本', '图书下架',
  '新增图书分类', '修改图书分类', '删除图书分类'
]

const tagType = (operation) => {
  if (operation === '停用账号') return 'danger'
  if (operation === '启用账号' || operation === '重置密码') return 'warning'
  if (['注册', '登录'].includes(operation)) return 'primary'
  if (['修改密码', '更新头像', '修改个人信息', '修改账号信息'].includes(operation)) return 'success'
  return 'info'
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLogs({
      page: page.value,
      size: size.value,
      username: searchForm.username || undefined,
      operation: searchForm.operation || undefined
    })
    logs.value = res.data.list
    total.value = res.data.total
  } finally { loading.value = false }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.operation = ''
  handleSearch()
}

onMounted(loadData)
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
