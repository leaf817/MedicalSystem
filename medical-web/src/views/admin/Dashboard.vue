<template>
  <div class="dashboard-page">
    <!-- 欢迎头部 -->
    <div class="dashboard-welcome">
      <el-avatar :size="48" class="avatar-admin">
        {{ (userInfo?.name || userInfo?.username || '管')[0] }}
      </el-avatar>
      <div>
        <div class="welcome-title">欢迎你，{{ userInfo?.name || userInfo?.username }}！</div>
        <div class="welcome-role">角色：{{ roleName }}</div>
      </div>
    </div>
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="dashboard-row">
      <el-col :span="6">
        <el-card class="stat-card stat-blue" shadow="hover">
          <i class="fa-solid fa-calendar-check stat-icon"></i>
          <div class="stat-num">{{ stats.todayAppointments }}</div>
          <div class="stat-desc">今日预约</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-green" shadow="hover">
          <i class="fa-solid fa-user-doctor stat-icon"></i>
          <div class="stat-num">{{ stats.todayVisits }}</div>
          <div class="stat-desc">今日就诊</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-orange" shadow="hover">
          <i class="fa-solid fa-pills stat-icon"></i>
          <div class="stat-num">{{ stats.pendingDispense }}</div>
          <div class="stat-desc">待发药</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card stat-red" shadow="hover">
          <i class="fa-solid fa-money-bill-wave stat-icon"></i>
          <div class="stat-num">¥{{ stats.todayIncome }}</div>
          <div class="stat-desc">今日收入</div>
        </el-card>
      </el-col>
    </el-row>
    <!-- 快捷入口 -->
    <el-row :gutter="18" class="dashboard-row quick-row">
      <el-col :span="6" v-for="item in quickEntries" :key="item.title">
        <el-card class="quick-card" @click="goto(item.path)">
          <i :class="item.icon + ' quick-icon'"></i>
          <span class="quick-title">{{ item.title }}</span>
        </el-card>
      </el-col>
    </el-row>
    <!-- 最新动态 -->
    <el-card class="activity-card">
      <div class="activity-title">最新动态</div>
      <el-timeline>
        <el-timeline-item
          v-for="(log, i) in activities"
          :key="i"
          :timestamp="log.time"
          :type="log.type"
        >
          {{ log.content }}
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const userInfo = computed(() => {
  try {
    return JSON.parse(sessionStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const roleName = computed(() => {
  const roles = userInfo.value?.roles || []
  const roleMap = {
    SUPER_ADMIN: '超级管理员',
    ADMIN: '医院管理员',
    DOCTOR: '医生',
    PATIENT: '患者'
  }
  return roles.length ? roleMap[roles[0]] || roles[0] : ''
})

const stats = ref({
  todayAppointments: 0,
  todayVisits: 0,
  pendingDispense: 0,
  todayIncome: 0
})

const quickEntries = [
  { title: '科室管理', icon: 'fa-solid fa-building', path: '/admin/dept' },
  { title: '医生管理', icon: 'fa-solid fa-user-doctor', path: '/admin/doctor' },
  { title: '预约管理', icon: 'fa-solid fa-calendar-check', path: '/admin/appointment' },
  { title: '药品管理', icon: 'fa-solid fa-pills', path: '/admin/medicine' }
]

const activities = ref([
  { time: '2025-03-19 10:00', content: '系统初始化完成，欢迎使用', type: 'primary' }
])

const goto = (path) => {
  router.push(path)
}
</script>

<style scoped>
.stat-icon {
  color: #217edf;
}
.stat-green .stat-icon {
  color: #20bf6b;
}
.stat-orange .stat-icon {
  color: #fa981e;
}
.stat-red .stat-icon {
  color: #f56c6c;
}
</style>
