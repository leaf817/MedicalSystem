<template>
  <div class="reception-appointment-page">
    <div class="patient-bar content-card">
      <div class="bar-title">
        <i class="fa-solid fa-user-check"></i>
        当前患者（现场挂号须先选择患者）
      </div>
      <div class="bar-body">
        <el-autocomplete
          v-model="searchKeyword"
          :fetch-suggestions="queryPatients"
          placeholder="姓名 / 手机号 / 身份证 / 患者编号"
          clearable
          class="patient-search"
          value-key="label"
          @select="onSelectPatient"
        >
          <template #default="{ item }">
            <div class="patient-option">
              <span class="opt-name">{{ item.name }}</span>
              <span class="opt-meta">{{ item.patientNo }} · {{ item.phone || '无手机号' }}</span>
            </div>
          </template>
        </el-autocomplete>
        <el-button type="primary" link @click="router.push('/reception/patient-register')">
          <i class="fa-solid fa-user-plus"></i> 新建患者
        </el-button>
      </div>
      <div v-if="selectedPatient" class="selected-patient">
        <el-tag type="success" size="large">
          {{ selectedPatient.name }}（{{ selectedPatient.patientNo }}）
        </el-tag>
        <el-button link type="danger" @click="clearPatient">清除</el-button>
      </div>
      <el-alert v-else type="warning" :closable="false" show-icon title="请先检索并选择患者，再进行预约挂号" />
    </div>

    <AppointmentBooking
      v-if="selectedPatient"
      mode="reception"
      :patient-id="selectedPatient.patientId"
      :patient-name="selectedPatient.name"
      @booked="onBooked"
    />
    <div v-else class="booking-placeholder content-card">
      <i class="fa-solid fa-calendar-xmark"></i>
      <p>选择患者后显示预约挂号向导</p>
    </div>

    <div class="list-section content-card">
      <div class="list-header">
        <h3><i class="fa-solid fa-list"></i> 今日预约</h3>
        <el-button link @click="loadTodayList"><i class="fa-solid fa-rotate"></i> 刷新</el-button>
      </div>
      <el-table :data="todayList" v-loading="listLoading" class="data-table">
        <el-table-column prop="appointmentNo" label="预约单号" min-width="160" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="deptName" label="科室" width="110" />
        <el-table-column prop="doctorName" label="医生" width="100" />
        <el-table-column label="就诊时间" width="170">
          <template #default="{ row }">{{ row.appointmentDate }} {{ row.timeSlot }}</template>
        </el-table-column>
        <el-table-column prop="statusText" label="状态" width="90" />
        <el-table-column label="费用" width="80">
          <template #default="{ row }">¥{{ row.feeAmount ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1 && row.paid === 0"
              link
              type="primary"
              @click="goCharge(row)"
            >收费</el-button>
            <el-button
              v-if="row.status === 1 && row.paid === 1 && !row.queueNo"
              link
              type="success"
              @click="checkinRow(row)"
            >签到</el-button>
            <el-button v-if="row.status === 1" link type="danger" @click="cancelRow(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppointmentBooking from '@/views/admin/AppointmentBooking.vue'
import {
  searchReceptionPatients,
  getReceptionPatient,
  getReceptionAppointmentPage,
  cancelReceptionAppointment,
  checkinReceptionAppointment
} from '@/api/reception'

const router = useRouter()
const route = useRoute()

const searchKeyword = ref('')
const selectedPatient = ref(null)
const todayList = ref([])
const listLoading = ref(false)

const todayStr = () => {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

const queryPatients = async (queryString, cb) => {
  if (!queryString?.trim()) {
    cb([])
    return
  }
  try {
    const list = await searchReceptionPatients(queryString.trim())
    cb((list || []).map(p => ({
      ...p,
      label: `${p.name}（${p.patientNo}）`
    })))
  } catch {
    cb([])
  }
}

const onSelectPatient = (item) => {
  selectedPatient.value = item
  searchKeyword.value = item.name
}

const clearPatient = () => {
  selectedPatient.value = null
  searchKeyword.value = ''
}

const loadTodayList = async () => {
  listLoading.value = true
  try {
    const res = await getReceptionAppointmentPage({
      current: 1,
      size: 50,
      date: todayStr()
    })
    todayList.value = res?.list || []
  } finally {
    listLoading.value = false
  }
}

const onBooked = () => {
  loadTodayList()
}

const goCharge = (row) => {
  router.push({
    path: '/reception/payment',
    query: { bizType: 'APPOINTMENT', bizId: row.appointmentId }
  })
}

const checkinRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确认为患者 ${row.patientName} 签到？`, '签到确认', { type: 'info' })
    await checkinReceptionAppointment(row.appointmentId)
    ElMessage.success('签到成功')
    loadTodayList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const cancelRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确定取消预约 ${row.appointmentNo}？`, '取消预约', { type: 'warning' })
    await cancelReceptionAppointment(row.appointmentId)
    ElMessage.success('已取消')
    loadTodayList()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const loadPatientFromQuery = async () => {
  const pid = route.query.patientId
  if (!pid) return
  try {
    const p = await getReceptionPatient(pid)
    if (p) {
      selectedPatient.value = p
      searchKeyword.value = p.name
    }
  } catch {
    /* ignore */
  }
}

watch(() => route.query.patientId, loadPatientFromQuery)

onMounted(() => {
  loadTodayList()
  loadPatientFromQuery()
})
</script>

<style scoped>
.reception-appointment-page {
  padding: 0 0 24px;
}
.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.patient-bar .bar-title {
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
}
.patient-bar .bar-title i {
  color: #0ea5e9;
  margin-right: 8px;
}
.bar-body {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.patient-search {
  flex: 1;
  max-width: 420px;
}
.patient-option {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
  padding: 4px 0;
}
.opt-name { font-weight: 500; }
.opt-meta { font-size: 12px; color: #94a3b8; }
.selected-patient {
  display: flex;
  align-items: center;
  gap: 12px;
}
.booking-placeholder {
  text-align: center;
  padding: 48px;
  color: #94a3b8;
}
.booking-placeholder i {
  font-size: 48px;
  margin-bottom: 12px;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
}
.list-header h3 i {
  color: #0ea5e9;
  margin-right: 8px;
}
</style>
