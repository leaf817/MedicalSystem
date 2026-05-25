<template>
  <div class="register-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-user-plus page-icon"></i>
        <div>
          <h2 class="page-title">患者建档</h2>
          <p class="page-desc">为到院患者创建账号与档案，建档后可进行预约挂号与收费</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="register-form">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="登录用户名，3～32 字符" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="患者真实姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="mobilePhone">
              <el-input v-model="form.mobilePhone" placeholder="11 位手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="可选" maxlength="18" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-select v-model="form.gender" placeholder="请选择" clearable style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker
                v-model="form.birthDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系地址" prop="address">
              <el-input v-model="form.address" placeholder="可选" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="form-actions">
          <el-button @click="resetForm">重置</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            <i class="fa-solid fa-check"></i> 提交建档
          </el-button>
        </div>
      </el-form>
    </div>

    <el-dialog v-model="successVisible" title="建档成功" width="420px" align-center>
      <div class="success-body" v-if="createdPatient">
        <p><strong>患者编号：</strong>{{ createdPatient.patientNo }}</p>
        <p><strong>姓名：</strong>{{ createdPatient.name }}</p>
        <p><strong>用户名：</strong>{{ form.username }}</p>
      </div>
      <template #footer>
        <el-button @click="successVisible = false">关闭</el-button>
        <el-button type="primary" @click="goAppointment">去预约挂号</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerReceptionPatient } from '@/api/reception'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)
const successVisible = ref(false)
const createdPatient = ref(null)

const form = reactive({
  username: '',
  password: '',
  name: '',
  mobilePhone: '',
  idCard: '',
  gender: '',
  birthDate: '',
  address: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度为 3～32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  mobilePhone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, {
    username: '',
    password: '',
    name: '',
    mobilePhone: '',
    idCard: '',
    gender: '',
    birthDate: '',
    address: ''
  })
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const res = await registerReceptionPatient({ ...form })
    createdPatient.value = res
    successVisible.value = true
    ElMessage.success('患者建档成功')
    resetForm()
  } finally {
    submitting.value = false
  }
}

const goAppointment = () => {
  successVisible.value = false
  if (createdPatient.value?.patientId) {
    router.push({
      path: '/reception/appointment',
      query: { patientId: createdPatient.value.patientId }
    })
  } else {
    router.push('/reception/appointment')
  }
}
</script>

<style scoped>
.register-page {
  padding: 24px 28px 32px;
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
  color: #0ea5e9;
  margin-top: 4px;
}
.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
}
.page-desc {
  font-size: 14px;
  color: #64748b;
  margin: 6px 0 0;
}
.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.register-form {
  max-width: 900px;
}
.form-actions {
  margin-top: 12px;
  padding-left: 100px;
}
.success-body p {
  margin: 8px 0;
  color: #334155;
}
</style>
