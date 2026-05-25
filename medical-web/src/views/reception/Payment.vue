<template>
  <div class="payment-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-cash-register page-icon"></i>
        <div>
          <h2 class="page-title">收费</h2>
          <p class="page-desc">办理挂号费、处方费等收费，支付成功后同步业务状态</p>
        </div>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="10">
        <div class="content-card">
          <el-tabs v-model="pendingTab" @tab-change="onTabChange">
            <el-tab-pane label="待收挂号费" name="appointment">
              <el-table
                :data="unpaidAppointments"
                v-loading="unpaidLoading"
                highlight-current-row
                @current-change="selectAppointment"
                max-height="380"
              >
                <el-table-column prop="appointmentNo" label="单号" min-width="130" />
                <el-table-column prop="patientName" label="患者" width="80" />
                <el-table-column label="费用" width="72">
                  <template #default="{ row }">¥{{ row.feeAmount }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="待收处方费" name="prescription">
              <el-table
                :data="unpaidPrescriptions"
                v-loading="unpaidRxLoading"
                highlight-current-row
                @current-change="selectPrescription"
                max-height="380"
              >
                <el-table-column prop="prescriptionNo" label="处方号" min-width="140" />
                <el-table-column prop="patientName" label="患者" width="80" />
                <el-table-column label="金额" width="72">
                  <template #default="{ row }">¥{{ row.totalAmount }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-col>
      <el-col :span="14">
        <div class="content-card charge-panel">
          <h3 class="card-title">收费办理</h3>
          <el-form :model="chargeForm" label-width="96px">
            <el-form-item label="业务类型">
              <el-select v-model="chargeForm.bizType" style="width: 100%" @change="onBizTypeChange">
                <el-option label="挂号费" value="APPOINTMENT" />
                <el-option label="处方费" value="PRESCRIPTION" />
              </el-select>
            </el-form-item>
            <el-form-item label="业务单号">
              <el-input v-model="chargeForm.bizId" placeholder="从左侧选择或手动输入 ID" />
            </el-form-item>
            <el-form-item label="收费金额">
              <el-input-number v-model="chargeForm.amount" :min="0" :precision="2" :step="1" style="width: 100%" />
              <div class="hint" v-if="selectedRow">
                参考：¥{{ selectedRow.feeAmount ?? selectedRow.totalAmount }}
              </div>
            </el-form-item>
            <el-form-item label="支付方式">
              <el-radio-group v-model="chargeForm.payMethod">
                <el-radio value="CASH">现金</el-radio>
                <el-radio value="WECHAT">微信</el-radio>
                <el-radio value="ALIPAY">支付宝</el-radio>
                <el-radio value="CARD">银行卡</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="chargeForm.remark" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="charging" @click="handleCharge">
                <i class="fa-solid fa-check"></i> 确认收费
              </el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="lastPaymentNo" type="success" :closable="false" class="pay-result">
            收费成功，流水号：<strong>{{ lastPaymentNo }}</strong>
          </el-alert>
        </div>
      </el-col>
    </el-row>

    <div class="content-card" style="margin-top: 16px">
      <h3 class="card-title">今日收费流水</h3>
      <el-table :data="paymentList" v-loading="paymentLoading">
        <el-table-column prop="paymentNo" label="流水号" min-width="160" />
        <el-table-column prop="patientName" label="患者" width="90" />
        <el-table-column prop="bizTypeText" label="类型" width="90" />
        <el-table-column prop="bizNo" label="业务单号" min-width="140" />
        <el-table-column label="金额" width="90">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="payMethodText" label="支付方式" width="90" />
        <el-table-column prop="payTime" label="支付时间" width="165" />
        <el-table-column prop="statusText" label="状态" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getReceptionAppointmentPage,
  chargePayment,
  getPaymentPage,
  getUnpaidPrescriptions
} from '@/api/reception'

const route = useRoute()
const pendingTab = ref('appointment')
const unpaidLoading = ref(false)
const unpaidRxLoading = ref(false)
const paymentLoading = ref(false)
const charging = ref(false)
const unpaidAppointments = ref([])
const unpaidPrescriptions = ref([])
const paymentList = ref([])
const selectedRow = ref(null)
const lastPaymentNo = ref('')

const chargeForm = reactive({
  bizType: 'APPOINTMENT',
  bizId: '',
  amount: null,
  payMethod: 'CASH',
  remark: ''
})

const todayStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const loadUnpaidAppointments = async () => {
  unpaidLoading.value = true
  try {
    const res = await getReceptionAppointmentPage({
      current: 1,
      size: 100,
      date: todayStr(),
      paid: 0,
      status: 1
    })
    unpaidAppointments.value = res?.list || []
  } finally {
    unpaidLoading.value = false
  }
}

const loadUnpaidPrescriptions = async () => {
  unpaidRxLoading.value = true
  try {
    const list = await getUnpaidPrescriptions()
    unpaidPrescriptions.value = Array.isArray(list) ? list : []
  } finally {
    unpaidRxLoading.value = false
  }
}

const loadPayments = async () => {
  paymentLoading.value = true
  try {
    const res = await getPaymentPage({
      current: 1,
      size: 20,
      dateFrom: todayStr(),
      dateTo: todayStr(),
      status: 1
    })
    paymentList.value = res?.list || []
  } finally {
    paymentLoading.value = false
  }
}

const selectAppointment = (row) => {
  if (!row) return
  selectedRow.value = row
  chargeForm.bizType = 'APPOINTMENT'
  chargeForm.bizId = String(row.appointmentId)
  chargeForm.amount = Number(row.feeAmount) || 0
  pendingTab.value = 'appointment'
}

const selectPrescription = (row) => {
  if (!row) return
  selectedRow.value = row
  chargeForm.bizType = 'PRESCRIPTION'
  chargeForm.bizId = String(row.prescriptionId)
  chargeForm.amount = Number(row.totalAmount) || 0
  pendingTab.value = 'prescription'
}

const onBizTypeChange = (type) => {
  selectedRow.value = null
  chargeForm.bizId = ''
  chargeForm.amount = null
  if (type === 'PRESCRIPTION') {
    loadUnpaidPrescriptions()
  } else {
    loadUnpaidAppointments()
  }
}

const onTabChange = (name) => {
  chargeForm.bizType = name === 'prescription' ? 'PRESCRIPTION' : 'APPOINTMENT'
  selectedRow.value = null
  if (name === 'prescription') {
    loadUnpaidPrescriptions()
  }
}

const handleCharge = async () => {
  if (!chargeForm.bizId) {
    ElMessage.warning('请填写或选择业务单')
    return
  }
  charging.value = true
  try {
    const res = await chargePayment({
      bizType: chargeForm.bizType,
      bizId: Number(chargeForm.bizId),
      payMethod: chargeForm.payMethod,
      amount: chargeForm.amount,
      remark: chargeForm.remark
    })
    lastPaymentNo.value = res?.paymentNo || ''
    ElMessage.success('收费成功')
    loadUnpaidAppointments()
    loadUnpaidPrescriptions()
    loadPayments()
  } finally {
    charging.value = false
  }
}

const applyRouteQuery = () => {
  if (route.query.bizType) {
    chargeForm.bizType = route.query.bizType
    pendingTab.value = route.query.bizType === 'PRESCRIPTION' ? 'prescription' : 'appointment'
  }
  if (route.query.bizId) chargeForm.bizId = String(route.query.bizId)
}

watch(() => route.query, applyRouteQuery, { immediate: true })

onMounted(() => {
  loadUnpaidAppointments()
  loadUnpaidPrescriptions()
  loadPayments()
})
</script>

<style scoped>
.payment-page {
  padding: 24px 28px 32px;
}
.page-header { margin-bottom: 20px; }
.header-left {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.page-icon { font-size: 28px; color: #22c55e; margin-top: 4px; }
.page-title { font-size: 22px; font-weight: 600; margin: 0; color: #1e293b; }
.page-desc { font-size: 14px; color: #64748b; margin: 6px 0 0; }
.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #334155;
}
.hint { font-size: 12px; color: #94a3b8; margin-top: 4px; }
.pay-result { margin-top: 12px; }
</style>
