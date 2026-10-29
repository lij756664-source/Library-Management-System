<template>
  <el-container class="layout-container">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <div class="logo-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M8 10C8 8.895 8.895 8 10 8H22V40H10C8.895 40 8 39.105 8 38V10Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M22 8H38C39.105 8 40 8.895 40 10V38C40 39.105 39.105 40 38 40H22V8Z" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
            <path d="M22 8V40" stroke="currentColor" stroke-width="1.8"/>
          </svg>
        </div>
        <span class="logo-text">图书借阅系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="side-menu"
        router
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-wrap">
      <el-header class="header">
        <span class="page-title">{{ pageTitle }}</span>
        <div class="header-right">
          <span class="role-badge" :class="authStore.isAdmin ? 'role-admin' : 'role-user'">
            {{ authStore.isAdmin ? '管理员' : '普通用户' }}
          </span>
          <span class="user-name">{{ authStore.nickname }}</span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { hasRole } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const allMenus = [
  { path: '/browse', title: '图书查询', icon: 'Search', roles: ['ADMIN', 'USER'] },
  { path: '/my-borrows', title: '我的借阅', icon: 'Notebook', roles: ['ADMIN', 'USER'] },
  { path: '/my-reservations', title: '我的预约', icon: 'Ticket', roles: ['ADMIN', 'USER'] },
  { path: '/books', title: '图书管理', icon: 'Collection', roles: ['ADMIN'] },
  { path: '/categories', title: '分类管理', icon: 'Menu', roles: ['ADMIN'] },
  { path: '/borrow-manage', title: '借阅管理', icon: 'List', roles: ['ADMIN'] },
  { path: '/reservation-manage', title: '预约管理', icon: 'Finished', roles: ['ADMIN'] },
  { path: '/statistics', title: '数据统计', icon: 'DataAnalysis', roles: ['ADMIN'] }
]

const menuItems = computed(() => allMenus.filter((m) => hasRole(authStore.role, m.roles)))
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '校园图书借阅系统')

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      authStore.clearAuth()
      router.push('/login')
    })
    .catch(() => {})
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
  background: var(--color-bg);
}

.aside {
  background: linear-gradient(180deg, #f5f9fc 0%, #eef6fb 100%);
  border-right: 1px solid var(--color-border-light);
  display: flex;
  flex-direction: column;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid var(--color-border-light);
}

.logo-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 8px rgba(100, 149, 180, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5b8db8;
  flex-shrink: 0;
}

.logo-icon svg {
  width: 20px;
  height: 20px;
}

.logo-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: 0.5px;
}

.side-menu {
  border-right: none;
  background: transparent;
  padding: 12px 10px;
  flex: 1;
}

.side-menu :deep(.el-menu-item) {
  height: 44px;
  line-height: 44px;
  margin-bottom: 4px;
  border-radius: 10px;
  color: var(--color-text-secondary);
  font-weight: 400;
  transition: all 0.2s ease;
}

.side-menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.7);
  color: var(--color-text);
}

.side-menu :deep(.el-menu-item.is-active) {
  background: #ffffff;
  color: var(--color-primary-hover);
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(100, 149, 180, 0.08);
}

.side-menu :deep(.el-menu-item .el-icon) {
  color: inherit;
}

.main-wrap {
  background: var(--color-bg);
}

.header {
  height: 60px;
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  box-shadow: 0 1px 0 var(--color-border-light);
}

.page-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-text);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.role-badge {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 400;
}

.role-admin {
  background: var(--color-primary-light);
  color: var(--color-primary-hover);
}

.role-user {
  background: var(--color-green-light);
  color: var(--color-green);
}

.user-name {
  font-size: 14px;
  color: var(--color-text-secondary);
}

.logout-btn {
  border: none;
  background: none;
  font-size: 13px;
  color: var(--color-text-muted);
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s ease, color 0.2s ease;
  font-family: inherit;
}

.logout-btn:hover {
  background: #f5f7f9;
  color: var(--color-danger-soft);
}

.main {
  padding: 24px;
}
</style>
