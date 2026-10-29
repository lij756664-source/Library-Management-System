<template>
  <el-card class="page-card" shadow="never">
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="书名/作者/ISBN" clearable />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable style="width: 150px">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadBooks">搜索</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="rulesText" :title="rulesText" type="info" show-icon :closable="false" class="rules-alert" />

    <el-table :data="books" v-loading="loading" stripe class="data-table">
      <el-table-column prop="title" label="书名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="categoryName" label="分类" width="100" align="center">
        <template #default="{ row }"><el-tag size="small">{{ row.categoryName }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="stock" label="可借库存" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.stock > 0 ? 'success' : 'danger'" size="small">{{ row.stock }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预约排队" width="100" align="center">
        <template #default="{ row }">
          <span v-if="row.waitingCount > 0">{{ row.waitingCount }} 人</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="简介" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.stock > 0" type="primary" link @click="handleBorrow(row)">借阅</el-button>
          <el-button v-else-if="row.myReserveStatus === 'READY'" type="warning" link disabled>待取书</el-button>
          <el-button v-else-if="row.myReserveStatus === 'WAITING'" type="info" link disabled>已预约</el-button>
          <el-button v-else type="warning" link @click="handleReserve(row)">预约</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getBooks } from '../api/book'
import { getCategories } from '../api/category'
import { borrowBook, getBorrowRules } from '../api/borrow'
import { reserveBook } from '../api/reservation'

const loading = ref(false)
const books = ref([])
const categories = ref([])
const rules = ref({})

const searchForm = reactive({ keyword: '', categoryId: null })

const rulesText = computed(() => {
  if (!rules.value.borrow_days) return ''
  return `借阅规则：默认借期 ${rules.value.borrow_days} 天，可续借 ${rules.value.max_renew_count} 次（每次 ${rules.value.renew_days} 天），逾期每天罚款 ${rules.value.fine_per_day} 元，最多同时借阅 ${rules.value.max_borrow_count} 册`
})

const loadBooks = async () => {
  loading.value = true
  try {
    const res = await getBooks({
      keyword: searchForm.keyword || undefined,
      categoryId: searchForm.categoryId || undefined
    })
    books.value = res.data
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  const res = await getCategories()
  categories.value = res.data
}

const loadRules = async () => {
  const res = await getBorrowRules()
  rules.value = res.data
}

const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.categoryId = null
  loadBooks()
}

const handleBorrow = (row) => {
  ElMessageBox.confirm(`确定借阅《${row.title}》吗？`, '借阅确认', { type: 'info' })
    .then(async () => {
      await borrowBook(row.id)
      ElMessage.success('借阅成功')
      loadBooks()
    })
    .catch(() => {})
}

const handleReserve = (row) => {
  ElMessageBox.confirm(`《${row.title}》暂无库存，确定加入预约队列吗？`, '预约确认', { type: 'info' })
    .then(async () => {
      await reserveBook(row.id)
      ElMessage.success('预约成功，已加入排队')
      loadBooks()
    })
    .catch(() => {})
}

onMounted(() => {
  loadBooks()
  loadCategories()
  loadRules()
})
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.rules-alert { margin-bottom: 16px; }
.text-muted { color: var(--color-text-muted); }
</style>
