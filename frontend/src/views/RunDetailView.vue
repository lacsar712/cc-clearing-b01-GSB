<template>
  <div class="page">
    <h2 class="page-title">批次详情</h2>
    <p class="page-desc">查看批次状态、参与义务、净头寸，并可确认 settle</p>

    <div class="toolbar">
      <el-button @click="$router.back()">返回</el-button>
      <el-button @click="load">刷新</el-button>
      <el-button
        type="success"
        :disabled="!auth.isOperator || detail?.run?.status !== 'COMPLETED' || alreadySettled"
        :loading="settling"
        @click="settle"
      >确认 Settle</el-button>
    </div>

    <div class="card-panel" v-loading="loading">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="Run ID"><span class="mono">{{ detail.run.runId }}</span></el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.run.status === 'COMPLETED' ? 'success' : detail.run.status === 'FAILED' ? 'danger' : 'info'">
              {{ detail.run.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="交割日">{{ detail.run.settleDate }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ detail.run.currency }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.run.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="ΣnetAmount">{{ detail.sumNetAmount }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.run.failureReason" label="失败原因" :span="2">
            {{ detail.run.failureReason }}
          </el-descriptions-item>
        </el-descriptions>

        <h3 style="margin:20px 0 10px">净头寸</h3>
        <el-table :data="detail.positions" stripe>
          <el-table-column prop="memberId" label="会员 ID" min-width="220">
            <template #default="{ row }"><span class="mono">{{ row.memberId }}</span></template>
          </el-table-column>
          <el-table-column prop="currency" label="币种" width="90" />
          <el-table-column prop="netAmount" label="净头寸" min-width="160" />
        </el-table>

        <h3 style="margin:20px 0 10px">参与义务</h3>
        <el-table :data="detail.obligations" stripe>
          <el-table-column prop="obligationId" label="义务 ID" min-width="200">
            <template #default="{ row }"><span class="mono">{{ row.obligationId }}</span></template>
          </el-table-column>
          <el-table-column prop="payerMemberId" label="付款方" min-width="180" />
          <el-table-column prop="payeeMemberId" label="收款方" min-width="180" />
          <el-table-column prop="amount" label="金额" width="140" />
          <el-table-column prop="status" label="状态" width="110" />
        </el-table>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const loading = ref(false)
const settling = ref(false)
const detail = ref(null)

const alreadySettled = computed(() =>
  (detail.value?.obligations || []).every((o) => o.status === 'SETTLED') &&
  (detail.value?.obligations || []).length > 0
)

function formatTime(v) {
  return v ? new Date(v).toLocaleString() : '-'
}

async function load() {
  loading.value = true
  try {
    const { data } = await api.get(`/netting-runs/${route.params.id}`)
    detail.value = data
  } finally {
    loading.value = false
  }
}

async function settle() {
  settling.value = true
  try {
    await api.post(`/netting-runs/${route.params.id}/settle`)
    ElMessage.success('Settle 完成，义务已 SETTLED')
    await load()
  } finally {
    settling.value = false
  }
}

onMounted(load)
</script>
