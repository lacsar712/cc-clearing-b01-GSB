import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue') },
      { path: 'members', name: 'members', component: () => import('../views/MembersView.vue') },
      { path: 'obligations', name: 'obligations', component: () => import('../views/ObligationsView.vue') },
      { path: 'netting', name: 'netting', component: () => import('../views/NettingView.vue') },
      { path: 'netting-runs/:id', name: 'run-detail', component: () => import('../views/RunDetailView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.token) {
    return { name: 'home' }
  }
})

export default router
