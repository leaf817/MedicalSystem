<template>
  <div class="login-page">
    <!-- 背景图 -->
    <div class="login-bg"></div>
    <!-- 主内容区 -->
    <div class="login-content">
      <!-- 顶部 logo 及标题 -->
      <div class="login-header">
        <div>
          <div class="login-title-zh">智能医疗服务管理系统</div>
          <div class="login-title-sub">Smart Medical Service System</div>
        </div>
      </div>
      <!-- 登录主卡片 -->
      <el-card class="login-card" shadow="never">
        <div class="login-tabs">
          <div class="tab active">账户密码登录</div>
        </div>
        <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        autocomplete="off"
        @keydown.enter.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账户名/工号"
            size="large"
            clearable
            autocomplete="off"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            autocomplete="new-password"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item style="margin-top: 26px">
          <el-button
            type="primary"
            class="login-btn"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            登&nbsp;&nbsp;录
          </el-button>
        </el-form-item>
        </el-form>
      </el-card>
      <!-- 页脚 -->
      <div class="login-footer">Copyright © 智能医疗服务管理系统 2026</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入账户名/工号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  await formRef.value?.validate().catch(() => {})
  loading.value = true
  try {
    const data = await login(form)
    sessionStorage.setItem('userInfo', JSON.stringify(data))
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/')
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 与背景图风格统一：温暖日落、传统与现代医疗融合 */
.login-page {
  min-height: 100vh;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  overflow: hidden;
}

/* 全屏背景图 */
.login-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: url('/images/login%20background.jpg') center center / cover no-repeat;
  z-index: 0;
}

/* 主内容层，置于背景之上 */
.login-content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding-top: 28px;
}

.login-header {
  display: flex;
  align-items: center;
  margin: 38px 0 18px 0;
  gap: 13px;
  user-select: none;
}

/* 标题：白色加粗描边，保证在深色背景上清晰可见 */
.login-title-zh {
  font-size: 26px;
  font-weight: bold;
  color: #fff;
  line-height: 32px;
  text-shadow:
    0 0 4px rgba(0, 0, 0, 0.9),
    0 0 8px rgba(0, 0, 0, 0.7),
    1px 1px 0 #000,
    -1px -1px 0 #000,
    1px -1px 0 #000,
    -1px 1px 0 #000,
    0 2px 4px rgba(0, 0, 0, 0.8);
}

.login-title-sub {
  font-size: 18px;
  color: #fff;
  font-family: "KaiTi", "楷体", cursive;
  margin-top: 2px;
  text-shadow:
    0 0 3px rgba(0, 0, 0, 0.9),
    0 0 6px rgba(0, 0, 0, 0.6),
    1px 1px 0 #000,
    -1px -1px 0 #000,
    0 1px 3px rgba(0, 0, 0, 0.8);
}

/* 毛玻璃透明卡片 */
.login-card {
  width: 400px;
  margin-top: 12px;
  border-radius: 16px;
  background: rgba(255, 252, 250, 0.35);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.login-card :deep(.el-card__body) {
  padding: 28px 32px;
}

.login-tabs {
  display: flex;
  margin: 6px 0 0 0;
  height: 38px;
}

.tab {
  width: 100%;
  text-align: center;
  font-size: 17px;
  font-weight: bold;
  line-height: 38px;
  color: #2c1810;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.9);
}

.login-form {
  padding: 20px 0 0 0;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

/* 输入框：半透明毛玻璃，保证可读 */
.login-form :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  background: rgba(255, 255, 255, 0.7) !important;
  backdrop-filter: blur(8px);
  border: 1.5px solid rgba(100, 70, 40, 0.5) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.login-form :deep(.el-input__wrapper:hover),
.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(210, 130, 50, 0.7) !important;
  box-shadow: 0 0 0 2px rgba(210, 130, 50, 0.15);
}

.login-form :deep(.el-input__inner) {
  color: #1a0f08;
  font-weight: 500;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #4a3520;
  font-weight: 500;
}

.login-form :deep(.el-icon) {
  color: #3d2914;
}

/* 落日暖橙登录按钮，白色文字加描边 */
.login-btn {
  width: 100%;
  font-size: 18px;
  font-weight: bold;
  background: linear-gradient(135deg, #e8a54b 0%, #d48232 100%);
  border: none;
  border-radius: 10px;
  letter-spacing: 3px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  box-shadow: 0 4px 16px rgba(212, 130, 50, 0.4);
}

.login-btn:hover,
.login-btn:focus {
  background: linear-gradient(135deg, #f0b55c 0%, #e08d3a 100%);
  box-shadow: 0 6px 20px rgba(212, 130, 50, 0.5);
}

.login-footer {
  text-align: center;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  margin: 35px 0 0 0;
  padding: 8px 16px;
  user-select: none;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  border-radius: 8px;
  text-shadow:
    0 0 2px rgba(0, 0, 0, 0.9),
    1px 1px 0 #000,
    -1px -1px 0 #000,
    0 1px 2px rgba(0, 0, 0, 0.8);
}
</style>
