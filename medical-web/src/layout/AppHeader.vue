<template>
  <el-header>
    <el-button @click="$emit('toggle-sidebar')" class="toggle-btn">
      <i class="fa-solid fa-bars"></i>
    </el-button>
    <h1>智能医疗服务管理系统</h1>
    <div class="header-right">
      <span class="role">{{ roleName }}</span>
      <span class="separator">|</span>
      <div>
        <el-avatar :size="36" class="avatar">
          {{ (userInfo?.name || userInfo?.username || '?')[0] }}
        </el-avatar>
        <span class="username">{{ userInfo?.name || userInfo?.username }}</span>
      </div>
      <span class="logout-btn" @click="handleLogout">
        <i class="fa-solid fa-arrow-right-from-bracket"></i>
      </span>
    </div>
  </el-header>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/auth'

const emit = defineEmits(['toggle-sidebar'])
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
    PATIENT: '患者',
    NURSE: '护士',
    RECEPTIONIST: '挂号员'
  }
  return roles.length ? roleMap[roles[0]] || roles[0] : ''
})

const handleLogout = async () => {
  try {
    await logout()
  } catch (e) {}
  sessionStorage.removeItem('userInfo')
  router.replace('/login')
  ElMessage.success('已退出登录')
}
</script>

<style scoped>
.toggle-btn {
  background-color: #344a5f;
  color: white;
  border: none;
  margin-right: 10px;
}
.toggle-btn:hover {
  background-color: #3d566e;
  color: white;
}
.avatar {
  background: linear-gradient(130deg, #5c9afb, #13d3b4);
  color: #fff;
}
.logout-btn {
  margin-left: 12px;
  cursor: pointer;
  font-size: 18px;
}
.logout-btn:hover {
  color: #ffd04b;
}
</style>
