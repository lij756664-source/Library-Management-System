import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { hasRole } from '../utils/auth'
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue'
import BookBrowse from '../views/BookBrowse.vue'
import MyBorrows from '../views/MyBorrows.vue'
import BookList from '../views/BookList.vue'
import CategoryList from '../views/CategoryList.vue'
import BorrowManage from '../views/BorrowManage.vue'
import Statistics from '../views/Statistics.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login, meta: { public: true } },
  {
    path: '/',
    component: Layout,
    redirect: '/browse',
    children: [
      { path: 'browse', name: 'BookBrowse', component: BookBrowse, meta: { title: '图书查询', roles: ['ADMIN', 'USER'] } },
      { path: 'my-borrows', name: 'MyBorrows', component: MyBorrows, meta: { title: '我的借阅', roles: ['ADMIN', 'USER'] } },
      { path: 'books', name: 'BookList', component: BookList, meta: { title: '图书管理', roles: ['ADMIN'] } },
      { path: 'categories', name: 'CategoryList', component: CategoryList, meta: { title: '分类管理', roles: ['ADMIN'] } },
      { path: 'borrow-manage', name: 'BorrowManage', component: BorrowManage, meta: { title: '借阅管理', roles: ['ADMIN'] } },
      { path: 'statistics', name: 'Statistics', component: Statistics, meta: { title: '数据统计', roles: ['ADMIN'] } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function getDefaultHome(authStore) {
  return authStore.isAdmin ? '/books' : '/browse'
}

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.public) {
    if (authStore.isLoggedIn) {
      next(getDefaultHome(authStore))
    } else {
      next()
    }
    return
  }

  if (!authStore.isLoggedIn) {
    next('/login')
    return
  }

  // 旧版缓存的登录信息可能没有 role，需重新登录
  if (!authStore.role) {
    authStore.clearAuth()
    next('/login')
    return
  }

  if (to.meta.roles && !hasRole(authStore.role, to.meta.roles)) {
    const fallback = getDefaultHome(authStore)
    if (to.path === fallback) {
      authStore.clearAuth()
      next('/login')
    } else {
      next(fallback)
    }
    return
  }

  next()
})

export default router
