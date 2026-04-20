<template>
  <div class="schedule-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-calendar-days page-icon"></i>
        <div>
          <h2 class="page-title">排班管理</h2>
          <p class="page-desc">维护医生排班，患者端“我要预约”会实时读取这里的数据。</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <el-select v-model="deptId" class="filter-select" placeholder="科室" clearable filterable style="width: 180px" @change="handleDeptChange">
          <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.name" :value="d.deptId" />
        </el-select>
        <el-select v-model="doctorId" class="filter-select" placeholder="医生" clearable filterable style="width: 180px" @change="loadData">
          <el-option v-for="d in doctorOptions" :key="d.doctorId" :label="`${d.name || d.username} (${d.username})`" :value="d.doctorId" />
        </el-select>
        <el-date-picker v-model="dateFilter" class="filter-select" type="date" value-format="YYYY-MM-DD" placeholder="排班日期" @change="loadData" />
        <el-select v-model="statusFilter" class="filter-select" placeholder="状态" clearable style="width: 120px" @change="loadData">
          <el-option :value="1" label="可预约" />
          <el-option :value="0" label="停诊" />
        </el-select>
        <el-button @click="handleDisableExpired">停用过期排班</el-button>
        <el-button class="add-btn" @click="openCreate">新增排班</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="data-table" :header-cell-style="headerCellStyle">
        <el-table-column prop="scheduleId" label="ID" width="72" />
        <el-table-column prop="deptName" label="科室" min-width="120" />
        <el-table-column prop="doctorName" label="医生" min-width="120">
          <template #default="{ row }">{{ row.doctorName || '-' }}{{ row.doctorTitle ? `（${row.doctorTitle}）` : '' }}</template>
        </el-table-column>
        <el-table-column prop="scheduleDate" label="日期" width="120" />
        <el-table-column prop="scheduleTimeSlot" label="时段" width="120" />
        <el-table-column prop="totalSlots" label="总号源" width="90" />
        <el-table-column prop="bookedSlots" label="已约" width="80" />
        <el-table-column prop="remainingSlots" label="剩余" width="80" />
        <el-table-column prop="statusText" label="状态" width="90" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑排班' : '新增排班'" width="520px" @close="resetForm">
      <el-radio-group v-if="!isEdit" v-model="dialogMode" style="margin-bottom: 12px">
        <el-radio-button label="single">单条新增</el-radio-button>
        <el-radio-button label="batch">一键排班</el-radio-button>
      </el-radio-group>

      <el-form v-if="isEdit || dialogMode === 'single'" ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="form.doctorId" filterable style="width: 100%">
            <el-option v-for="d in doctorOptions" :key="d.doctorId" :label="`${d.name || d.username} (${d.username})`" :value="d.doctorId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="scheduleDate">
          <el-date-picker
            v-model="form.scheduleDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled-date="disabledPastDate"
          />
        </el-form-item>
        <el-form-item label="时段" prop="timeSlot">
          <el-select v-model="form.timeSlot" style="width: 100%">
            <el-option v-for="slot in timeSlotOptions" :key="slot" :label="slot" :value="slot" />
          </el-select>
        </el-form-item>
        <el-form-item label="总号源" prop="totalSlots">
          <el-input-number v-model="form.totalSlots" :min="1" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option :value="1" label="可预约" />
            <el-option :value="0" label="停诊" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

        <el-form v-else ref="batchFormRef" :model="batchForm" :rules="batchRules" label-position="top">
          <el-form-item label="科室（可多选）" prop="deptIds">
          <el-select v-model="batchForm.deptIds" filterable clearable multiple collapse-tags style="width: 100%" @change="loadDialogDoctors">
            <el-option v-for="d in deptOptions" :key="d.deptId" :label="d.name" :value="d.deptId" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生（可多选）" prop="doctorIds">
          <el-select v-model="batchForm.doctorIds" filterable multiple collapse-tags style="width: 100%">
            <el-option
              v-for="d in dialogDoctorOptions"
              :key="d.doctorId"
              :label="`${d.name || d.username} (${d.username})`"
              :value="d.doctorId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围" required>
          <el-col :span="11">
            <el-form-item prop="startDate">
              <el-date-picker
                v-model="batchForm.startDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                placeholder="开始日期"
                :disabled-date="disabledPastDate"
              />
            </el-form-item>
          </el-col>
          <el-col :span="2" style="text-align: center; color: #909399">至</el-col>
          <el-col :span="11">
            <el-form-item prop="endDate">
              <el-date-picker
                v-model="batchForm.endDate"
                type="date"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                placeholder="结束日期"
                :disabled-date="disabledPastDate"
              />
            </el-form-item>
          </el-col>
        </el-form-item>
        <el-form-item label="时段（可多选）" prop="timeSlots">
          <el-select v-model="batchForm.timeSlots" multiple collapse-tags style="width: 100%">
            <el-option v-for="slot in timeSlotOptions" :key="slot" :label="slot" :value="slot" />
          </el-select>
        </el-form-item>
        <el-form-item label="总号源" prop="totalSlots">
          <el-input-number v-model="batchForm.totalSlots" :min="1" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="batchForm.status" style="width: 100%">
            <el-option :value="1" label="可预约" />
            <el-option :value="0" label="停诊" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="btn-cancel" @click="dialogVisible = false">取消</el-button>
        <el-button class="btn-save" :loading="saving" @click="submitForm">{{ isEdit || dialogMode === 'single' ? '保存' : '一键排班' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDeptOptions,
  getDoctorPage,
  getScheduleList,
  createSchedule,
  createScheduleBatch,
  updateSchedule,
  deleteSchedule,
  disableExpiredSchedules
} from '@/api/admin'

const loading = ref(false)
const saving = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref([])

const deptId = ref(null)
const doctorId = ref(null)
const dateFilter = ref('')
const statusFilter = ref(null)

const deptOptions = ref([])
const doctorOptions = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const dialogMode = ref('single')
const formRef = ref(null)
const batchFormRef = ref(null)
const dialogDoctorOptions = ref([])
const form = reactive({
  doctorId: null,
  scheduleDate: '',
  timeSlot: '08:00-09:00',
  totalSlots: 20,
  status: 1,
  remark: ''
})
const batchForm = reactive({
  deptIds: [],
  doctorIds: [],
  startDate: '',
  endDate: '',
  timeSlots: ['08:00-09:00'],
  totalSlots: 20,
  status: 1,
  remark: ''
})

const timeSlotOptions = ['08:00-09:00', '09:00-10:00', '10:00-11:00', '14:00-15:00', '15:00-16:00']

const headerCellStyle = {
  background: 'rgba(139, 90, 43, 0.08)',
  color: '#5c4a32',
  fontWeight: '600',
  fontSize: '13px',
  borderBottom: '1px solid rgba(139, 90, 43, 0.15)'
}

const todayStart = new Date()
todayStart.setHours(0, 0, 0, 0)
const disabledPastDate = (date) => date.getTime() < todayStart.getTime()

const rules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  scheduleDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择时段', trigger: 'change' }],
  totalSlots: [{ required: true, message: '请输入总号源', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}
const batchRules = {
  deptIds: [{ required: true, message: '请选择至少一个科室', trigger: 'change' }],
  doctorIds: [{ required: true, message: '请选择至少一个医生', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  timeSlots: [{ required: true, message: '请选择至少一个时段', trigger: 'change' }],
  totalSlots: [{ required: true, message: '请输入总号源', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const loadDepts = async () => {
  const list = await getDeptOptions()
  deptOptions.value = Array.isArray(list) ? list : []
}

const loadDoctors = async () => {
  const res = await getDoctorPage({
    current: 1,
    size: 200,
    status: 1,
    deptId: deptId.value ?? undefined
  })
  doctorOptions.value = res?.list || []
}

const loadDialogDoctors = async () => {
  const res = await getDoctorPage({
    current: 1,
    size: 200,
    status: 1
  })
  const all = res?.list || []
  if (batchForm.deptIds.length > 0) {
    dialogDoctorOptions.value = all.filter(d => batchForm.deptIds.includes(d.deptId))
  } else {
    dialogDoctorOptions.value = all
  }
  batchForm.doctorIds = batchForm.doctorIds.filter(id =>
    dialogDoctorOptions.value.some(d => d.doctorId === id)
  )
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getScheduleList({
      current: currentPage.value,
      size: pageSize.value,
      deptId: deptId.value ?? undefined,
      doctorId: doctorId.value ?? undefined,
      date: dateFilter.value || undefined,
      status: statusFilter.value ?? undefined
    })
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const handleDeptChange = async () => {
  doctorId.value = null
  await loadDoctors()
  currentPage.value = 1
  loadData()
}

const openCreate = () => {
  isEdit.value = false
  editingId.value = null
  dialogMode.value = 'single'
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  editingId.value = row.scheduleId
  form.doctorId = row.doctorId
  form.scheduleDate = row.scheduleDate
  form.timeSlot = row.scheduleTimeSlot
  form.totalSlots = row.totalSlots
  form.status = row.status
  form.remark = row.remark || ''
  dialogVisible.value = true
}

const resetForm = () => {
  form.doctorId = null
  form.scheduleDate = ''
  form.timeSlot = '08:00-09:00'
  form.totalSlots = 20
  form.status = 1
  form.remark = ''
  batchForm.deptIds = []
  batchForm.doctorIds = []
  batchForm.startDate = ''
  batchForm.endDate = ''
  batchForm.timeSlots = ['08:00-09:00']
  batchForm.totalSlots = 20
  batchForm.status = 1
  batchForm.remark = ''
  dialogDoctorOptions.value = []
  formRef.value?.resetFields()
  batchFormRef.value?.resetFields()
}

const submitForm = async () => {
  saving.value = true
  try {
    if (isEdit.value || dialogMode.value === 'single') {
      try {
        await formRef.value?.validate()
      } catch {
        return
      }
      const payload = {
        doctorId: form.doctorId,
        scheduleDate: form.scheduleDate,
        timeSlot: form.timeSlot,
        totalSlots: form.totalSlots,
        status: form.status,
        remark: form.remark || undefined
      }
      if (isEdit.value && editingId.value) {
        await updateSchedule(editingId.value, payload)
        ElMessage.success('更新成功')
      } else {
        await createSchedule(payload)
        ElMessage.success('创建成功')
      }
    } else {
      try {
        await batchFormRef.value?.validate()
      } catch {
        return
      }
      const res = await createScheduleBatch({
        deptIds: batchForm.deptIds,
        doctorIds: batchForm.doctorIds,
        startDate: batchForm.startDate,
        endDate: batchForm.endDate,
        timeSlots: batchForm.timeSlots,
        totalSlots: batchForm.totalSlots,
        status: batchForm.status,
        remark: batchForm.remark || undefined
      })
      ElMessage.success(`一键排班完成：新增 ${res?.createdCount ?? 0} 条，跳过 ${res?.skippedCount ?? 0} 条`)
    }
    dialogVisible.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const removeRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除排班（${row.scheduleDate} ${row.scheduleTimeSlot}）吗？`, '删除确认', {
      type: 'warning'
    })
  } catch {
    return
  }
  await deleteSchedule(row.scheduleId)
  ElMessage.success('删除成功')
  loadData()
}

const handleDisableExpired = async () => {
  try {
    await ElMessageBox.confirm(
      '将停用“已过期且无待就诊/已就诊预约”的排班，是否继续？',
      '操作确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  const res = await disableExpiredSchedules()
  const disabledCount = res?.disabledCount ?? 0
  const skippedCount = res?.skippedWithAppointments ?? 0
  ElMessage.success(`已停用 ${disabledCount} 条，因存在有效预约跳过 ${skippedCount} 条`)
  loadData()
}

onMounted(async () => {
  await loadDepts()
  await loadDoctors()
  await loadData()
})
</script>

<style scoped>
.schedule-page { padding: 24px 28px 32px; min-height: 100%; }
.page-header { margin-bottom: 20px; }
.header-left { display: flex; align-items: center; gap: 14px; }
.page-icon {
  width: 48px; height: 48px; line-height: 48px; text-align: center; font-size: 22px; color: #fff;
  background: linear-gradient(135deg, #8b6f47, #6b4f2a); border-radius: 12px;
  box-shadow: 0 4px 14px rgba(107, 79, 42, 0.35);
}
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #2c1810; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: #5c4a32; }
.content-card {
  background: rgba(255,252,248,.95); border-radius: 16px; padding: 20px 22px 24px;
  box-shadow: 0 8px 40px rgba(61,41,20,.08), 0 0 0 1px rgba(139,90,43,.08);
}
.toolbar { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; align-items: center; }

.filter-select :deep(.el-input__wrapper),
.filter-select :deep(.el-select__wrapper) {
  border-radius: 10px !important;
  background: rgba(255, 255, 255, 0.75) !important;
  border: 1px solid rgba(139, 90, 43, 0.2) !important;
  box-shadow: none !important;
}

.filter-select :deep(.el-input__wrapper:hover),
.filter-select :deep(.el-select__wrapper:hover),
.filter-select :deep(.el-input__wrapper.is-focus),
.filter-select :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(232, 165, 75, 0.6) !important;
  box-shadow: 0 0 0 2px rgba(232, 165, 75, 0.15) !important;
}

.add-btn {
  margin-left: auto;
  border-radius: 10px;
  background: linear-gradient(135deg, #e8a54b, #d48232);
  border: none;
  color: #fff;
  box-shadow: 0 4px 14px rgba(212, 130, 50, 0.3);
}

.data-table :deep(.el-table__row:hover > td) {
  background: rgba(232, 165, 75, 0.08) !important;
}

.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }

.btn-cancel { border-radius: 10px; }

.btn-save {
  border-radius: 10px;
  background: linear-gradient(135deg, #e8a54b, #d48232);
  border: none;
  color: #fff;
}
</style>
