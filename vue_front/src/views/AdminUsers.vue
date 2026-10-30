<template>
  <el-card class="page-card" shadow="never">
    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="关键词">
        <el-input v-model="searchForm.username" placeholder="用户名/昵称" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="searchForm.role" placeholder="全部" clearable style="width: 140px">
          <el-option label="图书管理员" value="ADMIN" />
          <el-option label="普通读者" value="USER" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="users" v-loading="loading" stripe class="data-table">
      <el-table-column label="头像" width="70" align="center">
        <template #default="{ row }">
          <el-avatar :size="36" :src="row.avatar || ''">
            <el-icon><UserFilled /></el-icon>
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="nickname" label="昵称" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">{{ row.email || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130">
        <template #default="{ row }">{{ row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column label="角色" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'warning' : 'success'" size="small">
            {{ row.role === 'ADMIN' ? '图书管理员' : '普通读者' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.role !== 'ADMIN'" type="primary" link @click="openEditDialog(row)">编辑</el-button>
          <el-button v-if="row.role !== 'ADMIN'" type="warning" link @click="openPasswordDialog(row)">重置密码</el-button>
          <el-button
            v-if="row.role !== 'ADMIN'"
            :type="row.status === 1 ? 'danger' : 'success'"
            link
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
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

    <!-- 编辑账号资料 -->
    <el-dialog v-model="editVisible" title="编辑账号资料" width="460px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="90px">
        <el-form-item label="用户名">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱（选填）" maxlength="100" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" placeholder="请输入手机号（选填）" maxlength="20" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleEditSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="passwordVisible" title="重置密码" width="420px" destroy-on-close>
      <p class="dialog-tip">为读者「{{ passwordTarget.username }}」设置新密码，重置后原密码失效。</p>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="6-20个字符" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="passwordForm.confirm" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePasswordSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, UserFilled } from '@element-plus/icons-vue'
import { getUsers, updateUserStatus, updateUserInfo, resetUserPassword } from '../api/user'
import { formatDateTime } from '../utils/date'

const loading = ref(false)
const submitting = ref(false)
const users = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const searchForm = reactive({ username: '', status: null, role: '' })

const editVisible = ref(false)
const editFormRef = ref(null)
const editForm = reactive({ id: null, username: '', nickname: '', email: '', phone: '' })

const passwordVisible = ref(false)
const passwordFormRef = ref(null)
const passwordTarget = ref({})
const passwordForm = reactive({ password: '', confirm: '' })

const editRules = {
  nickname: [{ max: 50, message: '昵称不能超过50字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ max: 20, message: '手机号不能超过20字符', trigger: 'blur' }]
}

const passwordRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6–20 字符', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getUsers({
      page: page.value,
      size: size.value,
      username: searchForm.username || undefined,
      status: searchForm.status ?? undefined,
      role: searchForm.role || undefined
    })
    users.value = res.data.list
    total.value = res.data.total
  } finally { loading.value = false }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.status = null
  searchForm.role = ''
  handleSearch()
}

const openEditDialog = (row) => {
  Object.assign(editForm, {
    id: row.id,
    username: row.username,
    nickname: row.nickname || '',
    email: row.email || '',
    phone: row.phone || ''
  })
  editVisible.value = true
}

const handleEditSubmit = async () => {
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await updateUserInfo(editForm.id, {
      nickname: editForm.nickname,
      email: editForm.email,
      phone: editForm.phone
    })
    ElMessage.success('账号资料已更新')
    editVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

const openPasswordDialog = (row) => {
  passwordTarget.value = row
  passwordForm.password = ''
  passwordForm.confirm = ''
  passwordVisible.value = true
}

const handlePasswordSubmit = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await resetUserPassword(passwordTarget.value.id, passwordForm.password)
    ElMessage.success('密码重置成功')
    passwordVisible.value = false
  } finally { submitting.value = false }
}

const handleToggleStatus = (row) => {
  const action = row.status === 1 ? '停用' : '启用'
  ElMessageBox.confirm(`确定${action}读者「${row.username}」的账号吗？${row.status === 1 ? '停用后该用户将无法登录。' : ''}`, `${action}确认`, { type: 'warning' })
    .then(async () => {
      await updateUserStatus(row.id, row.status === 1 ? 0 : 1)
      ElMessage.success(`账号已${action}`)
      loadData()
    })
    .catch(() => {})
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
.dialog-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
