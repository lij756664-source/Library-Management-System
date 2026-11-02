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
        <el-button type="primary" :icon="Search" @click="loadBooks">搜索</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增图书</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="books" v-loading="loading" stripe class="data-table">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="title" label="书名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="isbn" label="ISBN" width="150" />
      <el-table-column prop="categoryName" label="分类" width="100" align="center" />
      <el-table-column prop="location" label="馆藏位置" width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span :class="{ 'text-muted': !row.location }">{{ row.location || '未设置' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格(元)" width="100" align="right">
        <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="可借库存" width="160" align="center">
        <template #default="{ row }">
          <el-button
            link
            type="danger"
            :icon="Minus"
            :disabled="(row.stock ?? 0) <= 0"
            title="减少一册馆藏副本"
            @click="handleAdjustStock(row, -1)"
          />
          <span class="stock-num">{{ row.stock ?? 0 }}</span>
          <el-button
            link
            type="success"
            :icon="Plus"
            title="增加一册馆藏副本"
            @click="handleAdjustStock(row, 1)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑图书' : '新增图书'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="书名" prop="title">
          <el-input v-model="form.title" placeholder="请输入书名" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="作者" prop="author">
          <el-input v-model="form.author" placeholder="请输入作者" maxlength="100" />
        </el-form-item>
        <el-form-item label="ISBN" prop="isbn">
          <el-input v-model="form.isbn" placeholder="请输入ISBN，如 978-7-111-21382-6" maxlength="20" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :precision="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="馆藏位置" prop="location">
          <el-input v-model="form.location" placeholder="如：三楼社科阅览室 A-12 书架" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Minus, Edit, Delete } from '@element-plus/icons-vue'
import { getBooks, addBook, updateBook, deleteBook, adjustBookStock } from '../api/book'
import { getCategories } from '../api/category'

/** 检索关键词中不允许的特殊字符（Like 通配符与控制字符），避免非法检索参数 */
const ILLEGAL_KEYWORD = /[%_\\]/

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const books = ref([])
const categories = ref([])
const formRef = ref(null)

const searchForm = reactive({ keyword: '', categoryId: null })
const defaultForm = () => ({ id: null, title: '', author: '', isbn: '', categoryId: null, price: 0, stock: 0, location: '', description: '' })
const form = reactive(defaultForm())

/** ISBN 校验：去掉连字符与空格后应为 10 位（末位可为 X）或 13 位纯数字 */
const isValidIsbn = (value) => {
  const normalized = String(value || '').replace(/[-\s]/g, '')
  return /^\d{13}$/.test(normalized) || /^\d{9}[\dXx]$/.test(normalized)
}

const rules = {
  title: [
    { required: true, message: '请输入书名', trigger: 'blur' },
    { max: 200, message: '书名不能超过200字符', trigger: 'blur' },
    { validator: (_, value, cb) => (String(value || '').trim() ? cb() : cb(new Error('请输入书名'))), trigger: 'blur' }
  ],
  author: [
    { required: true, message: '请输入作者', trigger: 'blur' },
    { max: 100, message: '作者不能超过100字符', trigger: 'blur' },
    { validator: (_, value, cb) => (String(value || '').trim() ? cb() : cb(new Error('请输入作者'))), trigger: 'blur' }
  ],
  isbn: [
    { required: true, message: '请输入ISBN', trigger: 'blur' },
    { max: 20, message: 'ISBN 不能超过20字符', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        if (!String(value || '').trim()) return cb(new Error('请输入ISBN'))
        return isValidIsbn(value) ? cb() : cb(new Error('ISBN 格式不正确，应为 10 位或 13 位数字'))
      },
      trigger: 'blur'
    }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [
    { required: true, message: '请输入价格', trigger: 'blur' },
    { type: 'number', min: 0, message: '价格不能为负数', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入库存数量', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存不能为负数', trigger: 'blur' },
    { type: 'integer', message: '库存数量必须为整数', trigger: 'blur' }
  ],
  location: [
    { max: 100, message: '馆藏位置不能超过100字符', trigger: 'blur' }
  ]
}

const loadBooks = async () => {
  const keyword = String(searchForm.keyword || '').trim()
  if (ILLEGAL_KEYWORD.test(keyword)) {
    ElMessage.warning('搜索关键词不能包含 % _ \\ 等特殊字符，请重新输入')
    return
  }
  loading.value = true
  try {
    const res = await getBooks({
      keyword: keyword || undefined,
      categoryId: searchForm.categoryId || undefined
    })
    books.value = res.data || []
  } catch {
    // 异常提示已由请求拦截器统一处理，此处保证页面不崩溃
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

const resetSearch = () => { searchForm.keyword = ''; searchForm.categoryId = null; loadBooks() }

const openDialog = (row) => {
  isEdit.value = !!row
  Object.assign(form, row ? { ...row } : defaultForm())
  dialogVisible.value = true
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    // 提交前统一去除首尾空白，从源头拦截脏数据
    const payload = {
      ...form,
      title: String(form.title || '').trim(),
      author: String(form.author || '').trim(),
      isbn: String(form.isbn || '').trim(),
      location: String(form.location || '').trim(),
      description: String(form.description || '').trim()
    }
    if (isEdit.value) { await updateBook(form.id, payload); ElMessage.success('更新成功') }
    else { await addBook(payload); ElMessage.success('添加成功') }
    dialogVisible.value = false
    loadBooks()
  } catch {
    // 服务端校验不通过时，错误信息由请求拦截器弹出，弹窗保留供管理员修正
  } finally {
    submitting.value = false
  }
}

/** 馆藏副本维护：高危操作，二次确认后调用增减接口，库存最小值为 0 由后端兜底 */
const handleAdjustStock = async (row, delta) => {
  const current = row.stock ?? 0
  const action = delta > 0 ? '增加' : '减少'
  try {
    await ElMessageBox.confirm(
      `即将为《${row.title}》${action} 1 册馆藏副本：当前可借库存 ${current} 册，调整后 ${current + delta} 册。本次操作将记入操作日志，是否继续？`,
      '馆藏副本维护确认',
      { type: 'warning', confirmButtonText: '确认调整', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    const res = await adjustBookStock(row.id, delta)
    ElMessage.success(`库存调整成功，当前可借库存 ${res.data} 册`)
    loadBooks()
  } catch {
    // 库存为 0、图书已下架等异常由后端返回友好提示
  }
}

/** 图书下架：高危操作，二次确认；存在未完成借阅或排队预约时后端会拦截 */
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `下架将删除《${row.title}》的全部馆藏数据（当前可借库存 ${row.stock ?? 0} 册），且不可恢复。若该图书存在未完成的借阅记录或排队中的预约，系统将拦截本次操作。`,
      '高危操作确认',
      { type: 'warning', confirmButtonText: '确认下架', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteBook(row.id)
    ElMessage.success('下架成功')
    loadBooks()
  } catch {
    // 业务拦截提示由请求拦截器统一弹出
  }
}

onMounted(() => { loadBooks(); loadCategories() })
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
.stock-num { display: inline-block; min-width: 32px; text-align: center; font-weight: 600; }
.text-muted { color: var(--color-text-muted); }
</style>
