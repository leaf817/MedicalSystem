<template>
  <div class="my-appointment-page">
    <!-- 页面标题区域，保持原设计风格 -->
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-clock-rotate-left page-icon"></i>
        <div>
          <h2 class="page-title">我的预约</h2>
          <p class="page-desc">查看个人预约记录，支持取消待就诊预约与查看详情。</p>
        </div>
      </div>
      <el-button class="go-book-btn" @click="router.push('/patient/appointment')">
        <i class="fa-solid fa-calendar-plus"></i>
        去预约
      </el-button>
    </div>

    <!-- 内容卡片，采用玻璃毛玻璃效果（与药品列表一致） -->
    <div class="content-card">
      <div class="toolbar">
        <el-select v-model="statusFilter" clearable placeholder="预约状态" style="width: 140px" @change="applyFilters">
          <el-option label="待支付" value="unpaid" />
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-input
            v-model="keyword"
            clearable
            placeholder="搜索预约单号/医生/科室"
            style="width: 300px"
            @clear="applyFilters"
            @keyup.enter="applyFilters"
        >
          <template #prefix><i class="fa-solid fa-search"></i></template>
        </el-input>
      </div>

      <!-- 改为卡片布局，完全套用之前的卡片样式 -->
      <div v-if="filteredData.length === 0" class="empty-custom">
        <el-empty description="暂无预约记录" />
      </div>
      <div v-else>
        <div
            v-for="item in pagedData"
            :key="item.appointmentId"
            class="appointment-card"
        >
          <div class="card-header">
            <div class="hospital-info">
              <span class="dept-name">{{ item.deptName }}</span>
              <el-tag :type="getStatusType(item)" size="small">{{ getStatusText(item) }}</el-tag>
            </div>
            <div class="appointment-no">预约号：{{ item.appointmentNo }}</div>
          </div>

          <div class="card-body">
            <div class="doctor-info">
              <div class="doctor-avatar">
                <i class="fa-solid fa-user-md"></i>
              </div>
              <div class="doctor-detail">
                <div class="doctor-name">{{ item.doctorName }}</div>
                <div class="doctor-title">{{ item.doctorTitle || '医师' }}</div>
              </div>
            </div>

            <div class="appointment-info">
              <div class="info-row">
                <span class="info-label">就诊日期</span>
                <span class="info-value">{{ item.appointmentDate }} {{ item.timeSlot }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">就诊科室</span>
                <span class="info-value">{{ item.deptName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">挂号费用</span>
                <span class="info-value">
                  ¥{{ item.feeAmount }}
                  <span :class="item.paid === 1 ? 'paid' : 'unpaid'">
                    {{ item.paid === 1 ? '（已支付）' : '（未支付）' }}
                  </span>
                </span>
              </div>
              <div class="info-row">
                <span class="info-label">预约时间</span>
                <span class="info-value">{{ formatTime(item.createdTime) }}</span>
              </div>
            </div>
          </div>

          <div class="card-footer">
            <el-button
                v-if="item.status === 1"
                plain
                size="small"
                class="btn-cancel"
                @click="cancelRow(item)"
            >
              <i class="fa-solid fa-xmark"></i> 取消预约
            </el-button>
            <el-button
                v-if="item.status === 1 && item.paid === 0"
                plain
                size="small"
                class="btn-pay"
                @click="handlePay(item)"
            >
              <i class="fa-solid fa-credit-card"></i> 去支付
            </el-button>
            <span v-if="item.status !== 1" class="finished-tip">该预约已完成/取消</span>
          </div>
        </div>

        <!-- 分页组件 -->
        <div class="pagination-wrap">
          <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="filteredData.length"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="onPageSizeChange"
          />
        </div>
      </div>
    </div>

    <!-- 取消弹窗（沿用原卡片样式） -->
    <el-dialog
        v-model="cancelDialogVisible"
        title="取消预约"
        width="400px"
        :append-to-body="true"
        align-center
        class="medicine-dialog edit-dialog"
    >
      <p>确定要取消该预约吗？取消后号源将释放。</p>
      <template #footer>
        <div class="edit-dialog-footer">
          <el-button class="btn-cancel" @click="cancelDialogVisible = false">再想想</el-button>
          <el-button class="btn-save" type="danger" @click="confirmCancel">确认取消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情弹窗（保留原有详情样式，稍加润色） -->
    <el-dialog v-model="detailVisible" title="预约详情" width="560px" class="medicine-dialog edit-dialog">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="预约单号" :span="2">{{ detail.appointmentNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ detail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ detail.doctorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="医生职称">{{ detail.doctorTitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ detail.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ detail.appointmentDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时段">{{ detail.timeSlot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="排队号">{{ detail.queueNo ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="费用">¥{{ detail.feeAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ detail.paid === 1 ? '已支付' : '未支付' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detail.createdTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelAppointment, getAppointmentDetail, getMyAppointments, payAppointment } from '@/api/patient'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const filteredData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const statusFilter = ref(null)
const keyword = ref('')
const cancelDialogVisible = ref(false)
const currentCancelItem = ref(null)

const detailVisible = ref(false)
const detail = reactive({
  appointmentId: null,
  appointmentNo: '',
  patientName: '',
  doctorName: '',
  doctorTitle: '',
  deptName: '',
  appointmentDate: '',
  timeSlot: '',
  queueNo: null,
  status: null,
  statusText: '',
  feeAmount: null,
  paid: 0,
  createdTime: ''
})

const statusOptions = [
  { value: 1, label: '待就诊' },
  { value: 2, label: '已就诊' },
  { value: 3, label: '已取消' },
  { value: 4, label: '爽约' }
]

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

// 修改：根据 paid 字段判断是否待支付
const getStatusText = (item) => {
  if (item.status === 1 && item.paid === 0) return '待支付'
  const map = { 1: '待就诊', 2: '已就诊', 3: '已取消', 4: '爽约' }
  return map[item.status] || '未知'
}

// 修改：根据 paid 字段判断标签类型
const getStatusType = (item) => {
  if (item.status === 1 && item.paid === 0) return 'warning'
  const map = { 1: 'warning', 2: 'success', 3: 'info', 4: 'danger' }
  return map[item.status] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

// 传入筛选参数
const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (statusFilter.value === 'unpaid') {
      params.status = 1
      params.paid = 0
    } else if (statusFilter.value === 1) {
      // 待就诊：status=1 且 paid=1
      params.status = 1
      params.paid = 1
    } else if (statusFilter.value != null) {
      params.status = statusFilter.value
    }
    if (keyword.value.trim()) {
      params.keyword = keyword.value.trim()
    }
    const res = await getMyAppointments(params)
    list.value = Array.isArray(res) ? res : []
    // 后端已经排序和筛选好了，前端不需要再处理
    filteredData.value = list.value
    currentPage.value = 1
  } finally {
    loading.value = false
  }
}

// 筛选变化时重新加载
const applyFilters = () => {
  loadData()
}

const onPageSizeChange = () => {
  currentPage.value = 1
}

const cancelRow = async (row) => {
  if (row.status !== 1) return
  currentCancelItem.value = row
  cancelDialogVisible.value = true
}

const confirmCancel = async () => {
  try {
    await cancelAppointment(currentCancelItem.value.appointmentId)
    ElMessage.success('取消成功')
    cancelDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message || '取消失败，请稍后重试')
  }
}

const handlePay = async (item) => {
  try {
    await payAppointment(item.appointmentId)
    ElMessage.success('支付成功')
    await loadData()  // 刷新列表
  } catch (error) {
    ElMessage.error(error.message || '支付失败')
  }
}

const openDetail = async (row) => {
  const d = await getAppointmentDetail(row.appointmentId)
  detail.appointmentId = d.appointmentId
  detail.appointmentNo = d.appointmentNo || ''
  detail.patientName = d.patientName || ''
  detail.doctorName = d.doctorName || ''
  detail.doctorTitle = d.doctorTitle || ''
  detail.deptName = d.deptName || ''
  detail.appointmentDate = d.appointmentDate || ''
  detail.timeSlot = d.timeSlot || ''
  detail.queueNo = d.queueNo ?? null
  detail.status = d.status ?? null
  detail.statusText = d.statusText || ''
  detail.feeAmount = d.feeAmount ?? null
  detail.paid = d.paid ?? 0
  detail.createdTime = d.createdTime || ''
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
/* 完全和药品列表样式一致，同时融合原卡片风格 */
.my-appointment-page {
  padding: 24px 28px 32px;
  min-height: 100%;
}

.page-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
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
  background: linear-gradient(135deg, #e8a54b, #d48232);
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(212, 130, 50, 0.35);
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c1810;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.page-desc {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #5c4a32;
}

.go-book-btn {
  border-radius: 10px;
}

/* 药品列表同款玻璃卡片 */
.content-card {
  border-radius: 16px;
  background: rgba(255, 252, 250, 0.55);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 24px rgba(61, 41, 20, 0.1);
  overflow: hidden;
  padding: 24px 28px;
}

.empty-custom {
  padding: 60px 0;
  color: #b0a088;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 20px;
}

/* 卡片样式 */
.appointment-card {
  background: #fefaf5;
  border: 1px solid #f0e4d4;
  border-radius: 14px;
  margin-bottom: 16px;
  padding: 20px 24px;
  transition: all 0.2s ease;
}

.appointment-card:hover {
  border-color: #e8a54b;
  box-shadow: 0 4px 12px rgba(232, 165, 75, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0e4d4;
  margin-bottom: 15px;
}

.hospital-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dept-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c1810;
}

.appointment-no {
  font-size: 12px;
  color: #b0a088;
}

.card-body {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.doctor-info {
  display: flex;
  gap: 15px;
  align-items: center;
  min-width: 180px;
}

.doctor-avatar {
  width: 56px;
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-size: 28px;
  background: linear-gradient(135deg, #e8f4f0, #d4e8e0);
  border-radius: 50%;
  color: #2c7a5e;
}

.doctor-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c1810;
}

.doctor-title {
  font-size: 13px;
  color: #d48232;
}

.appointment-info {
  flex: 1;
}

.info-row {
  display: flex;
  margin-bottom: 10px;
  font-size: 14px;
  color: #2c1810;
}

.info-label {
  width: 80px;
  font-weight: 500;
  color: #8b7a68;
}

.info-value {
  flex: 1;
}

.paid {
  color: #67c23a;
  margin-left: 8px;
}

.unpaid {
  color: #f56c6c;
  margin-left: 8px;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 15px;
  border-top: 1px solid #f0e4d4;
  gap: 12px;
}

.finished-tip {
  font-size: 13px;
  color: #8b7a68;
}

/* 分页样式 */
.pagination-wrap {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* 按钮和药品列表一致 */
.btn-cancel {
  border-color: #f56c6c;
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
  border-radius: 10px;
}

.btn-cancel:hover {
  background: rgba(245, 108, 108, 0.2);
  border-color: #f56c6c;
  color: #f56c6c;
}

.btn-pay {
  border-color: #e8a54b;
  color: #d48232;
  background: rgba(232, 165, 75, 0.1);
  border-radius: 10px;
}

.btn-pay:hover {
  background: rgba(232, 165, 75, 0.2);
  border-color: #e8a54b;
  color: #d48232;
}

/* 弹窗样式完全一致 */
.medicine-dialog.edit-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  background: rgba(255, 252, 250, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 40px rgba(61, 41, 20, 0.15), 0 0 0 1px rgba(139, 90, 43, 0.08);
}

.medicine-dialog.edit-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid rgba(139, 90, 43, 0.12);
  background: rgba(255, 250, 245, 0.5);
}

.medicine-dialog.edit-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid rgba(139, 90, 43, 0.08);
}

.edit-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.btn-save {
  border-radius: 10px;
  padding: 10px 24px;
  border: none;
  color: #fff;
  background: linear-gradient(135deg, #e8a54b, #d48232);
  box-shadow: 0 4px 14px rgba(212, 130, 50, 0.3);
}
</style>