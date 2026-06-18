import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
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
      meta: { transition: 'transition-normal' },
    },
    {
      path: '/usuarios',
      name: 'usuarios',
      component: () => import('../Views/UserListView.vue'),
      meta: { transition: 'transition-normal' },
    },
    {
      path: '/arquivos',
      name: 'arquivos',
      component: () => import('../Views/FileListView.vue'),
      meta: { transition: 'transition-normal' },
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
      meta: { transition: 'transition-normal' },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('../Views/ChatRoom.vue'),
      meta: { transition: 'transition-normal' },
    },
  ],
})

export default router
