import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/views/Login.vue'
import Home from '@/views/Home.vue'
import Profile from '@/views/Profile.vue'
import ChangePassword from '@/views/ChangePassword.vue'
import AuditLog from '@/views/AuditLog.vue'
import { getAccessToken } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: Login,
      meta: { title: '登录' }
    },
    {
      path: '/',
      name: 'Home',
      component: Home,
      meta: { title: '用户管理', requiresAuth: true }
    },
    {
      path: '/audit-log',
      name: 'AuditLog',
      component: AuditLog,
      meta: { title: '操作审计日志', requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: Profile,
      meta: { title: '个人中心', requiresAuth: true }
    },
    {
      path: '/change-password',
      name: 'ChangePassword',
      component: ChangePassword,
      meta: { title: '修改密码', requiresAuth: true }
    }
  ]
})

router.beforeEach((to, from, next) => {
  const token = getAccessToken()
  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
