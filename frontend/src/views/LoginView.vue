<template>
  <div class="login-page">
    <div class="login-card">
      <h1>轧差清算工作台</h1>
      <p>多边净额清算演示 · 登录后继续</p>
      <el-form :model="form" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" style="width:100%" :loading="loading" native-type="submit">登录</el-button>
      </el-form>
      <div class="hint">operator / op123456 · viewer / view123456</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ username: 'operator', password: 'op123456' })

async function onSubmit() {
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/')
  } catch (e) {
    // interceptor shows message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 20% 20%, #d8f3dc 0, transparent 40%),
    radial-gradient(circle at 80% 0%, #cce3de 0, transparent 35%),
    linear-gradient(160deg, #f7fafc, #eef2f6);
}
.login-card {
  width: 380px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 28px 28px 22px;
  box-shadow: 0 12px 40px rgba(16, 42, 67, 0.08);
}
h1 {
  margin: 0 0 6px;
  font-size: 24px;
}
p {
  margin: 0 0 20px;
  color: var(--muted);
}
.hint {
  margin-top: 14px;
  font-size: 12px;
  color: var(--muted);
}
</style>
