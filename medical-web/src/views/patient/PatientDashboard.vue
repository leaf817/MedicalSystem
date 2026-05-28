<template>
  <div class="dashboard-page">
    <div class="dashboard-welcome">
      <el-avatar :size="48" class="avatar-patient">
        {{ (userInfo?.name || userInfo?.username || '患')[0] }}
      </el-avatar>
      <div>
        <div class="welcome-title">欢迎你，{{ userInfo?.name || userInfo?.username }}！</div>
        <div class="welcome-role">患者服务中心</div>
      </div>
    </div>

    <el-row :gutter="20" class="dashboard-row" v-loading="loading">
      <el-col :span="8">
        <el-card class="stat-card stat-blue" shadow="hover">
          <i class="fa-solid fa-calendar-check stat-icon"></i>
          <div class="stat-num">{{ stats.pendingAppointments ?? 0 }}</div>
          <div class="stat-desc">待就诊预约</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card stat-orange" shadow="hover">
          <i class="fa-solid fa-wallet stat-icon"></i>
          <div class="stat-num">{{ stats.unpaidCount ?? 0 }}</div>
          <div class="stat-desc">待支付</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card stat-green" shadow="hover">
          <i class="fa-solid fa-file-medical stat-icon"></i>
          <div class="stat-num">{{ stats.recentMedicalRecords ?? 0 }}</div>
          <div class="stat-desc">近30天病历</div>
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
import { getPatientDashboardStats } from '@/api/patient'

const router = useRouter()
const loading = ref(false)
const stats = ref({})

const userInfo = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const quickEntries = [
  { title: '我要预约', icon: 'fa-solid fa-calendar-plus', path: '/patient/appointment' },
  { title: '智能问诊', icon: 'fa-solid fa-robot', path: '/patient/ai-consult' },
  { title: '我的预约', icon: 'fa-solid fa-clock', path: '/patient/my-appointment' },
  { title: '我的病历', icon: 'fa-solid fa-file-medical', path: '/patient/medical-record' }
]

const loadStats = async () => {
  loading.value = true
  try {
    const res = await getPatientDashboardStats()
    stats.value = res || {}
  } finally {
    loading.value = false
  }
}

const goto = (path) => router.push(path)

onMounted(loadStats)
</script>

<style scoped>
.avatar-patient {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  font-weight: 600;
}
.stat-blue .stat-icon { color: #3b82f6; }
.stat-green .stat-icon { color: #22c55e; }
.stat-orange .stat-icon { color: #f59e0b; }
.stat-card {
  text-align: center;
  padding: 8px 0;
  border: none;
}
.stat-icon {
  font-size: 28px;
  margin-bottom: 8px;
}
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
  color: #6366f1;
  display: block;
  margin-bottom: 10px;
}
.quick-title {
  font-size: 15px;
  font-weight: 500;
  color: #334155;
}
</style>
