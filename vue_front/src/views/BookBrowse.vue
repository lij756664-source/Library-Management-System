<template>
  <el-card class="page-card" shadow="never">
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.keyword" placeholder="书名/作者/ISBN" clearable maxlength="100" @keyup.enter="loadBooks" />
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable style="width: 150px">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="searchForm.availableOnly">仅显示可借</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="loadBooks">搜索</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        <el-button @click="rulesVisible = true">查看最新规则</el-button>
      </el-form-item>
    </el-form>


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
      <el-table-column prop="location" label="馆藏位置" width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.location || '暂未标注' }}</template>
      </el-table-column>
      <el-table-column prop="description" label="简介" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="190" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="info" link :icon="View" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.stock > 0" type="primary" link @click="handleBorrow(row)">借阅</el-button>
          <el-button v-else-if="row.myReserveStatus === 'READY'" type="warning" link disabled>待取书</el-button>
          <el-button v-else-if="row.myReserveStatus === 'WAITING'" type="info" link disabled>已预约</el-button>
          <el-button v-else type="warning" link @click="handleReserve(row)">预约</el-button>
        </template>
      </el-table-column>
    </el-table>
    <BorrowRulesDialog v-model="rulesVisible" />

    <el-dialog v-model="detailVisible" title="图书详情" width="560px" destroy-on-close>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="书名">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ detail.author || '-' }}</el-descriptions-item>
            <el-descriptions-item label="ISBN">{{ detail.isbn || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="价格">¥{{ detail.price?.toFixed(2) ?? '0.00' }}</el-descriptions-item>
            <el-descriptions-item label="可借状态">
              <el-tag :type="detail.stock > 0 ? 'success' : 'danger'" size="small">
                {{ detail.stock > 0 ? `可借（剩余 ${detail.stock} 册）` : '暂无可借库存' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="馆藏位置">{{ detail.location || '暂未标注，可咨询馆员' }}</el-descriptions-item>
            <el-descriptions-item label="预约排队">{{ detail.waitingCount || 0 }} 人</el-descriptions-item>
            <el-descriptions-item label="简介">{{ detail.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </template>
        <el-empty v-else-if="!detailLoading" description="图书信息不存在或已下架" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, View } from '@element-plus/icons-vue'
import { getBooks, getBook } from '../api/book'
import { getCategories } from '../api/category'
import { borrowBook } from '../api/borrow'
import { reserveBook } from '../api/reservation'
import BorrowRulesDialog from '../components/BorrowRulesDialog.vue'

/** 检索关键词中不允许的特殊字符（Like 通配符与控制字符） */
const ILLEGAL_KEYWORD = /[%_\\]/

const loading = ref(false)
const books = ref([])
const categories = ref([])
const rulesVisible = ref(false)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

const searchForm = reactive({ keyword: '', categoryId: null, availableOnly: false })


const loadBooks = async () => {
  const keyword = String(searchForm.keyword || '').trim()
  // 搜索参数容错：空关键词按全部展示；含特殊字符时给出友好提示，不发起请求
  if (ILLEGAL_KEYWORD.test(keyword)) {
    ElMessage.warning('搜索关键词不能包含 % _ \\ 等特殊字符，请输入书名、作者或 ISBN')
    return
  }
  loading.value = true
  try {
    const res = await getBooks({
      keyword: keyword || undefined,
      categoryId: searchForm.categoryId || undefined,
      availableOnly: searchForm.availableOnly || undefined
    })
    books.value = res.data || []
  } catch {
    // 后端已返回友好提示，此处兜底保证页面不崩溃
    books.value = []
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}



const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.categoryId = null
  searchForm.availableOnly = false
  loadBooks()
}

/** 查看图书详情：编号非法、已下架或不存在时给出友好提示，不暴露底层报错 */
const openDetail = async (row) => {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    const res = await getBook(row.id)
    detail.value = res.data || null
    if (!detail.value) ElMessage.warning('图书信息不存在或已下架')
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
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
})
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.rules-alert { margin-bottom: 16px; }
.text-muted { color: var(--color-text-muted); }
</style>
