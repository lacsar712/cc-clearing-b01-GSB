<template>
  <div class="page">
    <h2 class="page-title">会员管理</h2>
    <p class="page-desc">清算会员列表、新建与启停（SUSPENDED 不可参与轧差）</p>

    <div class="toolbar">
      <el-button type="primary" :disabled="!auth.isOperator" @click="dialog = true">新建会员</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <div class="card-panel">
      <el-table :data="members" v-loading="loading" stripe>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="memberId" label="会员 ID" min-width="240">
          <template #default="{ row }"><span class="mono">{{ row.memberId }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button
              size="small"
              :disabled="!auth.isOperator || row.status === 'ACTIVE'"
              @click="setStatus(row, 'ACTIVE')"
            >启用</el-button>
            <el-button
              size="small"
              type="danger"
              :disabled="!auth.isOperator || row.status === 'SUSPENDED'"
              @click="setStatus(row, 'SUSPENDED')"
            >停用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialog" title="新建会员" width="420px">
      <el-form @submit.prevent>
        <el-form-item label="名称">
          <el-input v-model="newName" placeholder="例如 Alpha Bank" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const members = ref([])
const loading = ref(false)
const dialog = ref(false)
const newName = ref('')
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const { data } = await api.get('/members')
    members.value = data
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!newName.value.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  saving.value = true
  try {
    await api.post('/members', { name: newName.value.trim() })
    ElMessage.success('已创建')
    dialog.value = false
    newName.value = ''
    await load()
  } finally {
    saving.value = false
  }
}

async function setStatus(row, status) {
  await api.post(`/members/${row.memberId}/status`, { status })
  ElMessage.success('状态已更新')
  await load()
}

onMounted(load)
</script>
