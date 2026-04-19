<!-- medical-web/src/views/doctor/MedicalRecordList.vue -->
<template>
  <div class="medical-record-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-notes-medical page-icon"></i>
        <div>
          <h2 class="page-title">病历书写</h2>
          <p class="page-desc">管理患者病历，支持编辑和新增</p>
        </div>
      </div>
      <div class="header-right">
        <el-button type="primary" class="btn-create" @click="handleCreate">
          <i class="fa-solid fa-plus"></i> 新建病历
        </el-button>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card">
      <!-- 患者筛选 -->
      <div class="search-section">
        <div class="search-title">
          <i class="fa-solid fa-filter"></i>
          患者筛选
        </div>
        <div class="search-bar">
          <el-input
              v-model="searchKeyword"
              placeholder="输入患者姓名/手机号进行筛选"
              clearable
              prefix-icon="Search"
              class="search-input"
              @clear="clearSearch"
              @keyup.enter="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">
            <i class="fa-solid fa-magnifying-glass"></i> 查询
          </el-button>
          <el-button v-if="searchKeyword" @click="clearSearch">
            <i class="fa-solid fa-rotate-left"></i> 显示全部
          </el-button>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="stats-info">
        <span class="stats-text">
          <i class="fa-solid fa-file-alt"></i>
          共 {{ recordList.length }} 份病历
          <span v-if="selectedPatientName" class="filter-hint">
            （当前筛选：{{ selectedPatientName }}）
          </span>
        </span>
      </div>

      <!-- 病历列表 -->
      <div class="record-list" v-loading="loading">
        <div
            v-for="record in recordList"
            :key="record.recordId"
            class="record-card"
        >
          <div class="record-header">
            <div class="record-left">
              <div class="record-icon">
                <i class="fa-solid fa-file-medical"></i>
              </div>
              <div class="record-info">
                <div class="record-no">{{ record.recordNo }}</div>
                <div class="record-patient">
                  {{ record.patientName }}
                </div>
              </div>
            </div>
            <div class="record-right">
              <el-tag :type="record.status === 2 ? 'success' : 'info'" size="small">
                {{ record.status === 2 ? '已归档' : '草稿' }}
              </el-tag>
              <div class="record-date">
                <i class="fa-regular fa-calendar"></i>
                {{ record.visitDate }}
              </div>
            </div>
          </div>
          <div class="record-body">
            <div class="record-item">
              <span class="item-label">主诉：</span>
              <span class="item-value">{{ record.chiefComplaint || '--' }}</span>
            </div>
            <div class="record-item">
              <span class="item-label">诊断：</span>
              <span class="item-value diagnosis">{{ record.diagnosis || '--' }}</span>
            </div>
          </div>
          <div class="record-footer">
            <div class="footer-info">
              <i class="fa-regular fa-user"></i>
              医生：{{ record.doctorName || '--' }}
            </div>
            <div class="footer-actions">
              <el-button link type="primary" size="small" @click="handleEdit(record)">
                <i class="fa-solid fa-pen"></i> 编辑
              </el-button>
              <el-button link type="success" size="small" @click="handlePrescribe(record)">
                <i class="fa-solid fa-file-prescription"></i> 开处方
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(record)">
                <i class="fa-solid fa-trash"></i> 删除
              </el-button>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-if="recordList.length === 0 && !loading" class="empty-state">
          <i class="fa-solid fa-folder-open"></i>
          <p>暂无病历数据</p>
          <el-button type="primary" plain @click="handleCreate">立即创建</el-button>
        </div>
      </div>
    </div>

    <!-- 病历编辑对话框 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        :append-to-body="true"
        :modal-append-to-body="true"
        width="800px"
        class="record-dialog"
        destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <!-- 患者信息 -->
        <div class="form-section">
          <div class="section-title">患者信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="患者姓名" prop="patientId">
                <div class="patient-select">
                  <el-input v-model="form.patientName" placeholder="请选择患者" readonly />
                  <el-button type="primary" @click="openPatientDialog">
                    <i class="fa-solid fa-search"></i> 选择
                  </el-button>
                </div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="就诊日期" prop="visitDate">
                <el-date-picker
                    v-model="form.visitDate"
                    type="date"
                    placeholder="选择日期"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 病历信息 -->
        <div class="form-section">
          <div class="section-title-wrapper">
            <div class="section-title">病历信息</div>
            <el-button type="success" size="small" @click="handlePrescribeFromDialog" :disabled="!form.patientId">
              <i class="fa-solid fa-file-prescription"></i> 开处方
            </el-button>
          </div>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="主诉" prop="chiefComplaint">
                <el-input
                    v-model="form.chiefComplaint"
                    type="textarea"
                    :rows="2"
                    placeholder="例：发热、咳嗽3天，伴头痛"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="现病史" prop="presentIllness">
                <el-input
                    v-model="form.presentIllness"
                    type="textarea"
                    :rows="4"
                    placeholder="例：患者3天前无明显诱因出现发热，体温最高38.5℃，伴咳嗽、咳黄痰..."
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="既往史" prop="pastHistory">
                <el-input
                    v-model="form.pastHistory"
                    type="textarea"
                    :rows="2"
                    placeholder="例：高血压病史5年，糖尿病史2年，否认药物过敏史"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="体格检查" prop="physicalExam">
                <el-input
                    v-model="form.physicalExam"
                    type="textarea"
                    :rows="3"
                    placeholder="例：T 38.5℃，P 90次/分，R 20次/分，BP 120/80mmHg，咽部充血"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="诊断" prop="diagnosis">
                <el-input
                    v-model="form.diagnosis"
                    type="textarea"
                    :rows="3"
                    placeholder="例：1.急性上呼吸道感染 2.高血压病1级"
                />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="治疗计划" prop="treatmentPlan">
                <el-input
                    v-model="form.treatmentPlan"
                    type="textarea"
                    :rows="3"
                    placeholder="例：1.口服阿莫西林胶囊 0.5g tid 2.布洛芬缓释胶囊 0.3g prn"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 选择患者对话框 -->
    <el-dialog v-model="showPatientDialog" :append-to-body="true"  :modal-append-to-body="true" title="选择患者" width="700px" :z-index="3000" class="patient-dialog" @open="loadAllPatients">
      <div class="search-box">
        <el-input
            v-model="patientSearchKey"
            placeholder="输入患者姓名/手机号"
            clearable
            @keyup.enter="searchPatient"
        >
          <template #append>
            <el-button @click="searchPatient">搜索</el-button>
          </template>
        </el-input>
      </div>
      <div class="patient-list" v-loading="searchLoading">
        <div
            v-for="patient in patientList"
            :key="patient.patientId"
            class="patient-item"
            @click="selectPatient(patient)"
        >
          <div class="patient-avatar">
            <i class="fa-solid fa-user"></i>
          </div>
          <div class="patient-info">
            <div class="patient-name">{{ patient.name }}</div>
            <div class="patient-detail">
              <span>性别：{{ patient.gender === 'M' ? '男' : '女' }}</span>
              <span>电话：{{ patient.phone }}</span>
            </div>
          </div>
          <div class="patient-arrow">
            <i class="fa-solid fa-chevron-right"></i>
          </div>
        </div>
        <div v-if="patientList.length === 0 && !searchLoading" class="empty-state">
          <i class="fa-solid fa-user-slash"></i>
          <p>暂无患者数据</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAllMedicalRecords,
  getMedicalRecordDetail,
  saveMedicalRecord,
  deleteMedicalRecord,
  searchPatients,
  getAllPatients
} from '@/api/doctor'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('')
const selectedPatientName = ref('')
const recordList = ref([])
const allRecords = ref([])

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const showPatientDialog = ref(false)
const patientSearchKey = ref('')
const patientList = ref([])
const searchLoading = ref(false)
const isEdit = ref(false)

const form = reactive({
  recordId: null,
  patientId: null,
  patientName: '',
  visitDate: new Date().toISOString().split('T')[0],
  chiefComplaint: '',
  presentIllness: '',
  pastHistory: '',
  physicalExam: '',
  diagnosis: '',
  treatmentPlan: '',
  status: 2
})

const rules = {
  patientId: [{ required: true, message: '请选择患者', trigger: 'change' }],
  visitDate: [{ required: true, message: '请选择就诊日期', trigger: 'change' }],
  chiefComplaint: [{ required: true, message: '请输入主诉', trigger: 'blur' }],
  diagnosis: [{ required: true, message: '请输入诊断', trigger: 'blur' }]
}

// 加载所有病历
const loadAllRecords = async () => {
  loading.value = true
  try {
    const res = await getAllMedicalRecords()
    allRecords.value = res || []
    recordList.value = allRecords.value
  } catch (error) {
    ElMessage.error('加载病历列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索筛选
const handleSearch = async () => {
  if (!searchKeyword.value) {
    clearSearch()
    return
  }

  loading.value = true
  try {
    const res = await searchPatients(searchKeyword.value)
    const patients = res || []

    if (patients.length === 0) {
      ElMessage.warning('未找到相关患者')
      recordList.value = []
      selectedPatientName.value = ''
      return
    }

    const patientIds = patients.map(p => p.patientId)
    recordList.value = allRecords.value.filter(r => patientIds.includes(r.patientId))
    selectedPatientName.value = patients.map(p => p.name).join('、')

    if (recordList.value.length === 0) {
      ElMessage.info('该患者暂无病历记录')
    }
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

// 清除搜索，显示全部
const clearSearch = () => {
  searchKeyword.value = ''
  selectedPatientName.value = ''
  recordList.value = allRecords.value
}

// 新建病历
const handleCreate = () => {
  isEdit.value = false
  dialogTitle.value = '新建病历'
  resetForm()
  dialogVisible.value = true
}

// 编辑病历
const handleEdit = async (record) => {
  isEdit.value = true
  dialogTitle.value = '编辑病历'
  try {
    const res = await getMedicalRecordDetail(record.recordId)
    Object.assign(form, res)
    form.patientNo = record.patientNo
    form.patientId = record.patientId
    form.patientName = record.patientName
    dialogVisible.value = true
  } catch (error) {
    ElMessage.error('加载病历失败')
  }
}

// 删除病历
const handleDelete = async (record) => {
  try {
    await ElMessageBox.confirm('确定要删除这份病历吗？删除后无法恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteMedicalRecord(record.recordId)
    ElMessage.success('删除成功')
    loadAllRecords()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 开处方（列表中的） - 传递 recordId
const handlePrescribe = async (record) => {
  router.push({
    path: '/doctor/prescription',
    query: {
      recordId: record.recordId,
      patientNo: record.patientNo
    }
  })
}

// 开处方（模态框中的） - 传递 recordId
// 开处方（模态框中的）
const handlePrescribeFromDialog = async () => {
  if (!form.patientId) {
    ElMessage.warning('请先选择患者')
    return
  }

  // 如果没有 recordId（新建病历），先保存获取 recordId
  if (!form.recordId) {
    try {
      await formRef.value.validate()
      const res = await saveMedicalRecord(form)
      if (res && res.recordId) {
        form.recordId = res.recordId
        form.patientNo = res.patientNo
        ElMessage.success('病历已保存')
      } else {
        ElMessage.error('保存病历失败')
        return
      }
    } catch (error) {
      ElMessage.error('请填写完整的病历信息')
      return
    }
  } else {
    // 已有病历，直接保存更新
    try {
      await saveMedicalRecord(form)
    } catch (error) {
      ElMessage.error('保存病历失败')
      return
    }
  }

  // 关闭对话框
  dialogVisible.value = false

  // 从 form 中获取 patientNo，而不是 record
  router.push({
    path: '/doctor/prescription',
    query: {
      recordId: form.recordId,
      patientNo: form.patientNo
    }
  })
}

// 保存病历
const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true
    await saveMedicalRecord(form)
    ElMessage.success(isEdit.value ? '修改成功' : '创建成功')
    dialogVisible.value = false
    loadAllRecords()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 打开患者选择对话框并加载所有患者
const openPatientDialog = () => {
  patientSearchKey.value = ''
  showPatientDialog.value = true
  loadAllPatients()
}

// 加载所有患者
const loadAllPatients = async () => {
  searchLoading.value = true
  try {
    const res = await getAllPatients()
    patientList.value = res || []
  } catch (error) {
    ElMessage.error('加载患者列表失败')
  } finally {
    searchLoading.value = false
  }
}

// 搜索患者
const searchPatient = async () => {
  searchLoading.value = true
  try {
    let res
    if (!patientSearchKey.value) {
      res = await getAllPatients()
    } else {
      res = await searchPatients(patientSearchKey.value)
    }
    patientList.value = res || []
  } catch (error) {
    ElMessage.error('搜索患者失败')
  } finally {
    searchLoading.value = false
  }
}

// 选择患者
const selectPatient = (patient) => {
  form.patientId = patient.patientId
  form.patientName = patient.name
  form.patientNo = patient.patientNo
  showPatientDialog.value = false
  patientList.value = []
  patientSearchKey.value = ''
}

// 重置表单
const resetForm = () => {
  form.recordId = null
  form.patientId = null
  form.patientName = ''
  form.visitDate = new Date().toISOString().split('T')[0]
  form.chiefComplaint = ''
  form.presentIllness = ''
  form.pastHistory = ''
  form.physicalExam = ''
  form.diagnosis = ''
  form.treatmentPlan = ''
  form.status = 2
}

onMounted(() => {
  loadAllRecords()
})
</script>

<style scoped>
.medical-record-page {
  padding: 24px 28px 32px;
  min-height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-icon {
  width: 48px;
  height: 48px;
  line-height: 48px;
  text-align: center;
  font-size: 22px;
  color: #fff;
  background: linear-gradient(135deg, #2c7a5e, #1e5a45);
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(44, 122, 94, 0.35);
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c1810;
}

.page-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #5c4a32;
}

.btn-create {
  background: linear-gradient(135deg, #2c7a5e, #1e5a45);
  border: none;
  border-radius: 10px;
}

.content-card {
  border-radius: 16px;
  background: rgba(255, 252, 250, 0.55);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 24px rgba(61, 41, 20, 0.1);
  overflow: hidden;
  padding: 24px 28px;
}

.search-section {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0e4d4;
}

.search-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c1810;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-bar {
  display: flex;
  gap: 12px;
}

.search-input {
  width: 300px;
}

.stats-info {
  margin-bottom: 20px;
  padding: 8px 0;
}

.stats-text {
  font-size: 13px;
  color: #8b7a68;
}

.filter-hint {
  color: #2c7a5e;
  font-weight: 500;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-card {
  background: #fefaf5;
  border: 1px solid #f0e4d4;
  border-radius: 12px;
  padding: 14px 18px;
  transition: all 0.2s;
}

.record-card:hover {
  border-color: #2c7a5e;
  box-shadow: 0 4px 12px rgba(44, 122, 94, 0.1);
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.record-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.record-icon {
  width: 36px;
  height: 36px;
  line-height: 36px;
  text-align: center;
  font-size: 16px;
  background: linear-gradient(135deg, #e8f4f0, #d4e8e0);
  border-radius: 8px;
  color: #2c7a5e;
}

.record-no {
  font-size: 13px;
  font-weight: 600;
  color: #2c1810;
}

.record-patient {
  font-size: 12px;
  color: #8b7a68;
  margin-top: 2px;
}

.record-right {
  text-align: right;
}

.record-date {
  font-size: 12px;
  color: #b0a088;
  margin-top: 4px;
}

.record-body {
  margin-bottom: 10px;
  padding-left: 48px;
}

.record-item {
  font-size: 13px;
  line-height: 1.7;
}

.item-label {
  color: #8b7a68;
}

.item-value {
  color: #2c1810;
}

.item-value.diagnosis {
  color: #d48232;
  font-weight: 500;
}

.record-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-left: 48px;
}

.footer-info {
  font-size: 12px;
  color: #b0a088;
}

.footer-actions {
  display: flex;
  gap: 12px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #b0a088;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 12px;
}

.form-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c1810;
  margin-bottom: 16px;
  padding-left: 8px;
  border-left: 3px solid #2c7a5e;
}

.patient-select {
  display: flex;
  gap: 10px;
  width: 100%;
}

.patient-select .el-input {
  flex: 1;
}

.patient-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.patient-list {
  max-height: 400px;
  overflow-y: auto;
}

.patient-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px;
  background: #fefaf5;
  border: 1px solid #f0e4d4;
  border-radius: 10px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.patient-item:hover {
  border-color: #2c7a5e;
  background: #fff8f0;
}

.patient-avatar i {
  font-size: 32px;
  color: #2c7a5e;
}

.patient-info {
  flex: 1;
}

.patient-name {
  font-size: 15px;
  font-weight: 600;
  color: #2c1810;
  margin-bottom: 4px;
}

.patient-detail {
  font-size: 12px;
  color: #8b7a68;
  display: flex;
  gap: 16px;
}

.patient-arrow {
  color: #b0a088;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.section-title-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #2c1810;
  padding-left: 8px;
  border-left: 3px solid #2c7a5e;
  margin-bottom: 0;
}

</style>