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
        <el-button type="success" :icon="Plus" @click="openDialog()">新增图书</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="books" v-loading="loading" stripe class="data-table">
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="title" label="书名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="author" label="作者" width="120" />
      <el-table-column prop="isbn" label="ISBN" width="150" />
      <el-table-column prop="categoryName" label="分类" width="100" align="center" />
      <el-table-column prop="price" label="价格(元)" width="100" align="right">
        <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" align="center" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
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
          <el-input v-model="form.isbn" placeholder="请输入ISBN" maxlength="20" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
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
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getBooks, addBook, updateBook, deleteBook } from '../api/book'
import { getCategories } from '../api/category'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const books = ref([])
const categories = ref([])
const formRef = ref(null)

const searchForm = reactive({ keyword: '', categoryId: null })
const defaultForm = () => ({ id: null, title: '', author: '', isbn: '', categoryId: null, price: 0, stock: 0, description: '' })
const form = reactive(defaultForm())

const rules = {
  title: [
    { required: true, message: '请输入书名', trigger: 'blur' },
    { max: 200, message: '书名不能超过200字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ type: 'number', min: 0, message: '价格不能为负数', trigger: 'blur' }],
  stock: [{ type: 'number', min: 0, message: '库存不能为负数', trigger: 'blur' }]
}

const loadBooks = async () => {
  loading.value = true
  try {
    const res = await getBooks({ keyword: searchForm.keyword || undefined, categoryId: searchForm.categoryId || undefined })
    books.value = res.data
  } finally { loading.value = false }
}

const loadCategories = async () => {
  const res = await getCategories()
  categories.value = res.data
}

const resetSearch = () => { searchForm.keyword = ''; searchForm.categoryId = null; loadBooks() }

const openDialog = (row) => {
  isEdit.value = !!row
  Object.assign(form, row ? { ...row } : defaultForm())
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) { await updateBook(form.id, form); ElMessage.success('更新成功') }
    else { await addBook(form); ElMessage.success('添加成功') }
    dialogVisible.value = false
    loadBooks()
  } finally { submitting.value = false }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除《${row.title}》吗？`, '删除确认', { type: 'warning' })
    .then(async () => { await deleteBook(row.id); ElMessage.success('删除成功'); loadBooks() })
    .catch(() => {})
}

onMounted(() => { loadBooks(); loadCategories() })
</script>

<style scoped>
.search-form { margin-bottom: 16px; }
</style>
