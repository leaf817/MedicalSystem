<template>
  <div class="dashboard-page">
    <div class="dashboard-welcome">
      <el-avatar :size="48" class="avatar-reception">
        {{ (userInfo?.name || userInfo?.username || '挂')[0] }}
      </el-avatar>
      <div>
        <div class="welcome-title">欢迎你，{{ userInfo?.name || userInfo?.username }}！</div>
        <div class="welcome-role">挂号收费工作台</div>
      </div>
    </div>

    <el-row :gutter="20" class="dashboard-row" v-loading="loading">
      <el-col :span="6">
        <el-card class="stat-card stat-blue" shadow="hover">
          <i class="fa-solid fa-calendar-plus stat-icon"></i>
          <div class="stat-num">{{ summary.todayAppointments ?? 0 }}</div>
          <div class="stat-desc">今日预约</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-orange" shadow="hover">
          <i class="fa-solid fa-clock stat-icon"></i>
          <div class="stat-num">{{ summary.pendingPayment ?? 0 }}</div>
          <div class="stat-desc">待收费</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-green" shadow="hover">
          <i class="fa-solid fa-money-bill-wave stat-icon"></i>
          <div class="stat-num">¥{{ formatMoney(summary.todayPaidAmount) }}</div>
          <div class="stat-desc">今日已收</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-red" shadow="hover">
          <i class="fa-solid fa-rotate-left stat-icon"></i>
          <div class="stat-num">{{ summary.todayRefunds ?? 0 }}</div>
          <div class="stat-desc">今日退费</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18" class="dashboard-row quick-row">
      <el-col :span="6" v-for="item in quickEntries" :key="item.title">
        <el-card class="quick-card" @click="goto(item.path)">
          <i :class="item.icon + ' quick-icon'"></i>
          <span class="quick-title">{{ item.title }}</span>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getReceptionDashboardSummary } from '@/api/reception'

const router = useRouter()
const loading = ref(false)
const summary = ref({})

const userInfo = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const quickEntries = [
  { title: '现场挂号', icon: 'fa-solid fa-calendar-plus', path: '/reception/appointment' },
  { title: '患者建档', icon: 'fa-solid fa-user-plus', path: '/reception/patient-register' },
  { title: '收费', icon: 'fa-solid fa-cash-register', path: '/reception/payment' },
  { title: '退费', icon: 'fa-solid fa-rotate-left', path: '/reception/refund' }
]

const formatMoney = (val) => {
  const n = Number(val)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

const loadSummary = async () => {
  loading.value = true
  try {
    const res = await getReceptionDashboardSummary()
    summary.value = res || {}
  } finally {
    loading.value = false
  }
}

const goto = (path) => router.push(path)

onMounted(loadSummary)
</script>

<style scoped>
.dashboard-page {
  padding: 24px 28px 32px;
}
.dashboard-welcome {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.avatar-reception {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #fff;
  font-weight: 600;
}
.welcome-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
}
.welcome-role {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
}
.dashboard-row {
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
  padding: 8px 0;
  border: none;
}
.stat-icon {
  font-size: 28px;
  margin-bottom: 8px;
}
.stat-blue .stat-icon { color: #3b82f6; }
.stat-green .stat-icon { color: #22c55e; }
.stat-orange .stat-icon { color: #f59e0b; }
.stat-red .stat-icon { color: #ef4444; }
.stat-num {
  font-size: 26px;
  font-weight: 700;
  color: #1e293b;
}
.stat-desc {
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}
.quick-card {
  cursor: pointer;
  text-align: center;
  padding: 20px 12px;
  transition: transform 0.2s;
}
.quick-card:hover {
  transform: translateY(-2px);
}
.quick-icon {
  font-size: 32px;
  color: #0ea5e9;
  display: block;
  margin-bottom: 10px;
}
.quick-title {
  font-size: 15px;
  font-weight: 500;
  color: #334155;
}
</style>
