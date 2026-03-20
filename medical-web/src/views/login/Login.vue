<template>
  <div class="login-page">
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
    <div class="login-footer">Copyright © 智能医疗服务管理系统 2025</div>
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
/* 参考 book-library login.css */
.login-page {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
}

.login-header {
  display: flex;
  align-items: center;
  margin: 38px 0 18px 0;
  gap: 13px;
  user-select: none;
}

.login-title-zh {
  font-size: 24px;
  font-weight: bold;
  color: #2469b8;
  line-height: 32px;
}

.login-title-sub {
  font-size: 18px;
  color: #222;
  font-family: "KaiTi", cursive;
  margin-top: 2px;
}

.login-card {
  width: 400px;
  margin-top: 12px;
  border-radius: 12px;
  box-shadow: none;
  border: 1.5px solid #e8eef2;
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
  color: #19c4b3;
}

.login-form {
  padding: 20px 0 0 0;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 7px !important;
  background: #fff !important;
  border: 1px solid #c2e1e9 !important;
}

.login-btn {
  width: 100%;
  font-size: 18px;
  font-weight: bold;
  background: #19c4b3;
  border: none;
  border-radius: 7px;
  letter-spacing: 3px;
}

.login-btn:hover,
.login-btn:focus {
  filter: brightness(96%);
}

.login-footer {
  text-align: center;
  color: #b6bdc5;
  font-size: 13px;
  margin: 35px 0 0 0;
  user-select: none;
}
</style>
