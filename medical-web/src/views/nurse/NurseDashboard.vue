<template>
  <div class="dashboard-page">
    <div class="dashboard-welcome">
      <el-avatar :size="48" class="avatar-nurse">
        {{ (userInfo?.name || userInfo?.username || '护')[0] }}
      </el-avatar>
      <div>
        <div class="welcome-title">欢迎你，{{ userInfo?.name || userInfo?.username }}！</div>
        <div class="welcome-role">药房工作台</div>
      </div>
    </div>

    <el-row :gutter="20" class="dashboard-row" v-loading="loading">
      <el-col :span="8">
        <el-card class="stat-card stat-orange" shadow="hover" @click="goto('/nurse/prescription')">
          <i class="fa-solid fa-pills stat-icon"></i>
          <div class="stat-num">{{ stats.pendingDispense ?? 0 }}</div>
          <div class="stat-desc">待发药</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="stat-card stat-green" shadow="hover" @click="goto('/nurse/dispense')">
          <i class="fa-solid fa-circle-check stat-icon"></i>
          <div class="stat-num">{{ stats.todayDispensed ?? 0 }}</div>
          <div class="stat-desc">今日已发药</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card
          class="stat-card stat-red"
          shadow="hover"
          :class="{ 'has-warning': (stats.stockWarningCount ?? 0) > 0 }"
          @click="goto('/nurse/inventory')"
        >
          <el-badge
            :value="stats.stockWarningCount ?? 0"
            :hidden="!(stats.stockWarningCount > 0)"
            class="warning-badge"
          >
            <i class="fa-solid fa-triangle-exclamation stat-icon"></i>
          </el-badge>
          <div class="stat-num">{{ stats.stockWarningCount ?? 0 }}</div>
          <div class="stat-desc">低库存药品</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18" class="dashboard-row quick-row">
      <el-col :span="8" v-for="item in quickEntries" :key="item.title">
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
import { getNurseDashboardStats } from '@/api/nurse'

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
  { title: '待发药', icon: 'fa-solid fa-list', path: '/nurse/prescription' },
  { title: '发药确认', icon: 'fa-solid fa-circle-check', path: '/nurse/dispense' },
  { title: '药品盘点', icon: 'fa-solid fa-boxes-stacked', path: '/nurse/inventory' }
]

const loadStats = async () => {
  loading.value = true
  try {
    const res = await getNurseDashboardStats()
    stats.value = res || {}
  } finally {
    loading.value = false
  }
}

const goto = (path) => router.push(path)

onMounted(loadStats)
</script>

<style scoped>
.avatar-nurse {
  background: linear-gradient(135deg, #ec4899, #db2777);
  color: #fff;
  font-weight: 600;
}
.stat-orange .stat-icon { color: #f59e0b; }
.stat-green .stat-icon { color: #22c55e; }
.stat-red .stat-icon { color: #ef4444; }
.stat-card {
  text-align: center;
  padding: 8px 0;
  border: none;
  cursor: pointer;
}
.stat-card.has-warning {
  box-shadow: 0 0 0 2px rgba(239, 68, 68, 0.35);
}
.warning-badge :deep(.el-badge__content) {
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
  color: #ec4899;
  display: block;
  margin-bottom: 10px;
}
.quick-title {
  font-size: 15px;
  font-weight: 500;
  color: #334155;
}
</style>
