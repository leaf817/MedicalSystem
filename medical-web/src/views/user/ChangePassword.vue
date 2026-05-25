<template>
  <div class="change-password-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-key page-icon"></i>
        <div>
          <h2 class="page-title">修改密码</h2>
          <p class="page-desc">修改当前登录账号的登录密码，修改成功后请使用新密码登录</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="password-form"
        @keydown.enter.prevent="handleSubmit"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
            maxlength="64"
            autocomplete="current-password"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="6～64 个字符"
            maxlength="64"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
            maxlength="64"
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            <i class="fa-solid fa-check"></i> 确认修改
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert type="info" :closable="false" show-icon class="tips">
        密码将使用 BCrypt 加密存储。修改成功后不会自动退出，建议在公共设备上修改后重新登录。
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword } from '@/api/user'

const formRef = ref()
const submitting = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (_rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6～64 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, { oldPassword: '', newPassword: '', confirmPassword: '' })
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    await changePassword({ ...form })
    ElMessage.success('密码修改成功')
    resetForm()
  } catch (e) {
    if (e?.message) {
      ElMessage.error(e.message)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.change-password-page {
  padding: 24px 28px 32px;
  min-height: 100%;
}
.page-header {
  margin-bottom: 20px;
}
.header-left {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.page-icon {
  font-size: 28px;
  color: #d97706;
  margin-top: 4px;
}
.page-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0;
  color: #1e293b;
}
.page-desc {
  font-size: 14px;
  color: #64748b;
  margin: 6px 0 0;
}
.content-card {
  background: rgba(255, 252, 248, 0.95);
  border-radius: 16px;
  padding: 28px 32px;
  max-width: 520px;
  box-shadow: 0 8px 40px rgba(61, 41, 20, 0.08), 0 0 0 1px rgba(139, 90, 43, 0.08);
}
.password-form {
  margin-bottom: 16px;
}
.tips {
  margin-top: 8px;
}
</style>
