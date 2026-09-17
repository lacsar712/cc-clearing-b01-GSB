<template>
  <div class="page">
    <h2 class="page-title">工作台首页</h2>
    <p class="page-desc">今日待轧差义务摘要与最近轧差批次</p>

    <div class="summary card-panel">
      <div class="stat">
        <div class="label">OPEN 义务笔数</div>
        <div class="value">{{ openCount }}</div>
      </div>
      <div class="stat">
        <div class="label">OPEN 义务总金额</div>
        <div class="value">{{ openAmount }}</div>
      </div>
      <div class="stat">
        <div class="label">涉及币种</div>
        <div class="value">{{ currencies || '-' }}</div>
      </div>
    </div>

    <div class="card-panel" style="margin-top:16px">
      <div class="toolbar" style="justify-content:space-between">
        <strong>最近轧差批次</strong>
        <el-button type="primary" @click="$router.push('/netting')">去执行轧差</el-button>
      </div>
      <el-table :data="runs" stripe v-loading="loading">
        <el-table-column prop="runId" label="Run ID" min-width="220">
          <template #default="{ row }">
            <router-link class="mono" :to="`/netting-runs/${row.runId}`">{{ row.runId }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="settleDate" label="交割日" width="120" />
        <el-table-column prop="currency" label="币种" width="90" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '../api/client'

const loading = ref(false)
const opens = ref([])
const runs = ref([])

const openCount = computed(() => opens.value.length)
const openAmount = computed(() =>
  opens.value.reduce((s, o) => s + Number(o.amount || 0), 0).toLocaleString(undefined, { maximumFractionDigits: 2 })
)
const currencies = computed(() => [...new Set(opens.value.map((o) => o.currency))].join(', '))

function statusType(s) {
  if (s === 'COMPLETED') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'warning'
  return 'info'
}

function formatTime(v) {
  if (!v) return '-'
  return new Date(v).toLocaleString()
}

async function load() {
  loading.value = true
  try {
    const [o, r] = await Promise.all([
      api.get('/obligations', { params: { status: 'OPEN' } }),
      api.get('/netting-runs')
    ])
    opens.value = o.data
    runs.value = r.data.slice(0, 20)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.stat .label {
  color: var(--muted);
  font-size: 13px;
}
.stat .value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
}
@media (max-width: 900px) {
  .summary {
    grid-template-columns: 1fr;
  }
}
</style>
