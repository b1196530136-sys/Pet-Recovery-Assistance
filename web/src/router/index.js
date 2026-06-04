import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/',
    component: () => import('@/components/layout/FrontLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/home/HomePage.vue') },
      { path: 'posts', name: 'PostList', component: () => import('@/views/post/PostList.vue') },
      { path: 'posts/:id', name: 'PostDetail', component: () => import('@/views/post/PostDetail.vue') },
      { path: 'posts/create', name: 'PostCreate', component: () => import('@/views/post/PostCreate.vue') },
      { path: 'archives', name: 'ArchiveList', component: () => import('@/views/archive/ArchiveList.vue') },
      { path: 'archives/:id', name: 'ArchiveDetail', component: () => import('@/views/archive/ArchiveDetail.vue') },
      { path: 'archives/create', name: 'ArchiveCreate', component: () => import('@/views/archive/ArchiveCreate.vue') },
      { path: 'messages', name: 'MessageCenter', component: () => import('@/views/message/MessageCenter.vue') },
    ],
  },
  {
    path: '/auth',
    component: () => import('@/components/layout/BlankLayout.vue'),
    children: [
      { path: 'login', name: 'Login', component: () => import('@/views/auth/LoginPage.vue') },
      { path: 'register', name: 'Register', component: () => import('@/views/auth/RegisterPage.vue') },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/components/layout/AdminLayout.vue'),
    beforeEnter: requireAdmin,
    children: [
      { path: '', name: 'AdminDashboard', component: () => import('@/views/admin/DashboardPage.vue') },
      { path: 'posts', name: 'AdminPostReview', component: () => import('@/views/admin/PostReview.vue') },
      { path: 'archives', name: 'AdminArchiveReview', component: () => import('@/views/admin/ArchiveReview.vue') },
      { path: 'certifications', name: 'AdminCertReview', component: () => import('@/views/admin/CertReview.vue') },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/NotFoundPage.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

let sessionRestored = false

function requireAdmin(to, from, next) {
  const userStore = useUserStore()
  if (!userStore.isLoggedIn) {
    return next('/auth/login')
  }
  if (!userStore.isAdmin) {
    return next('/')
  }
  next()
}

router.beforeEach((to, from, next) => {
  if (!sessionRestored) {
    const userStore = useUserStore()
    userStore.restoreSession()
    sessionRestored = true
  }
  next()
})

export default router
