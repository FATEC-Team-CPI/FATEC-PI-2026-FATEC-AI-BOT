import { createRouter, createWebHistory } from 'vue-router'
import { clearStoredToken, getStoredToken, validateStoredToken } from '../services/backendApi'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/chat',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../Views/LoginView.vue'),
      meta: { transition: 'transition-normal' },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../Views/RegisterView.vue'),
      meta: { transition: 'transition-normal' },
    },
    {
      path: '/menu',
      name: 'menu',
      component: () => import('../Views/MenuView.vue'),
      meta: { transition: 'transition-normal' },
    },
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../Views/AdminView.vue'),
      meta: { transition: 'transition-normal', requiresAuth: true },
    },
    {
      path: '/usuarios',
      name: 'usuarios',
      component: () => import('../Views/UserListView.vue'),
      meta: { transition: 'transition-normal', requiresAuth: true },
    },
    {
      path: '/arquivos',
      name: 'arquivos',
      component: () => import('../Views/FileListView.vue'),
      meta: { transition: 'transition-normal', requiresAuth: true },
    },
    {
      path: '/password-reset',
      name: 'reset',
      component: () => import('../Views/ResetPassword.vue'),
      meta: { transition: 'transition-normal' },
    },
    {
      path: '/docs',
      name: 'docs',
      component: () => import('../Views/DocSelect.vue'),
      meta: { transition: 'transition-normal', requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../Views/ChatRoom.vue'),
      meta: { transition: 'transition-normal' },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/chat',
    },
  ],
})

router.beforeEach(async (to) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth === true)

  if (!requiresAuth) {
    return true
  }

  const token = getStoredToken()
  if (!token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  try {
    await validateStoredToken()
    return true
  } catch {
    clearStoredToken()
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }
})

export default router
