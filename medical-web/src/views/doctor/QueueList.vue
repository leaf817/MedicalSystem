<template>
  <div class="queue-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-users-viewfinder page-icon"></i>
        <div>
          <h2 class="page-title">待诊队列</h2>
          <p class="page-desc">管理今日待就诊患者，支持叫号、接诊、完成就诊（按时间段排序）</p>
        </div>
      </div>
      <div class="header-right">
        <el-button class="btn-refresh" @click="refreshData" :loading="refreshing">
          <i class="fa-solid fa-rotate-right"></i> 刷新
        </el-button>
      </div>
    </div>

    <!-- 日期切换标签 -->
    <div class="date-tabs">
      <div
          v-for="date in availableDates"
          :key="date.date"
          class="date-tab"
          :class="{ active: currentDate === date.date }"
          @click="switchDate(date.date)"
      >
        <div class="date-display">{{ date.display }}</div>
        <div class="date-week">{{ date.weekday }}</div>
        <div class="date-count" :class="{ empty: date.count === 0 }">
          {{ date.count > 0 ? date.count + '个预约' : '暂无预约' }}
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" :class="{ active: filterStatus === null }" @click="setFilter(null)">
        <div class="stat-icon total">
          <i class="fa-solid fa-calendar-day"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">今日预约</div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: filterStatus === 'waiting' }" @click="setFilter('waiting')">
        <div class="stat-icon waiting">
          <i class="fa-solid fa-hourglass-half"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value waiting-value">{{ stats.waiting }}</div>
          <div class="stat-label">待就诊</div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: filterStatus === 'completed' }" @click="setFilter('completed')">
        <div class="stat-icon completed">
          <i class="fa-solid fa-circle-check"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.completed }}</div>
          <div class="stat-label">已就诊</div>
        </div>
      </div>
      <div class="stat-card" :class="{ active: filterStatus === 'cancelled' }" @click="setFilter('cancelled')">
        <div class="stat-icon cancelled">
          <i class="fa-solid fa-ban"></i>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.cancelled }}</div>
          <div class="stat-label">已取消</div>
        </div>
      </div>
    </div>

    <!-- 搜索栏和排队信息 -->
    <div class="search-bar">
      <el-input
          v-model="keyword"
          placeholder="搜索患者姓名/病历号/预约号"
          clearable
          prefix-icon="Search"
          class="search-input"
          @clear="loadQueueList"
          @keyup.enter="loadQueueList"
      />
      <div class="queue-info-card">
        <div class="queue-info-item">
          <span class="label">当前叫号</span>
          <span class="value current-queue">{{ currentCalling?.queueNo || '--' }}</span>
          <span class="name">{{ currentCalling?.patientName || '暂无' }}</span>
          <span class="time-slot-badge" v-if="currentCalling?.timeSlot">{{ currentCalling.timeSlot }}</span>
        </div>
        <div class="queue-info-item">
          <span class="label">等候人数</span>
          <span class="value waiting-count">{{ waitingQueueCount }}</span>
        </div>
        <div class="queue-info-item" v-if="nextWaiting">
          <span class="label">下一位</span>
          <span class="value">{{ nextWaiting.queueNo || '--' }}号</span>
          <span class="name">{{ nextWaiting.patientName || '' }}</span>
          <span class="time-slot-badge" v-if="nextWaiting.timeSlot">{{ nextWaiting.timeSlot }}</span>
        </div>
        <el-button
            type="success"
            size="small"
            class="call-btn"
            :loading="autoCalling"
            :disabled="waitingQueueCount === 0 || autoCalling"
            @click="autoCallNext"
        >
          <i class="fa-solid fa-bullhorn"></i> 叫号
        </el-button>
      </div>
    </div>

    <!-- 队列列表 -->
    <div class="queue-container">
      <div class="queue-tabs">
        <div
            class="tab-item"
            :class="{ active: activeTab === 'waiting' }"
            @click="activeTab = 'waiting'; loadQueueList()"
        >
          <i class="fa-solid fa-clock"></i> 待就诊队列
          <span class="badge">{{ waitingCount }}</span>
        </div>
        <div
            class="tab-item"
            :class="{ active: activeTab === 'completed' }"
            @click="activeTab = 'completed'; loadCompletedList()"
        >
          <i class="fa-solid fa-check-circle"></i> 已完成就诊
          <span class="badge">{{ stats.completed || 0 }}</span>
        </div>
        <div
            class="tab-item"
            :class="{ active: activeTab === 'cancelled' }"
            @click="activeTab = 'cancelled'; loadCancelledList()"
        >
          <i class="fa-solid fa-ban"></i> 已取消
          <span class="badge">{{ stats.cancelled || 0 }}</span>
        </div>
      </div>

      <div class="queue-list" v-loading="loading">
        <!-- 待就诊分组：按时段分组显示，只显示已签到的人 -->
        <div v-if="activeTab === 'waiting' && groupedByTimeSlot.length > 0">
          <div
              v-for="group in groupedByTimeSlot"
              :key="group.timeSlot"
              class="time-slot-group"
          >
            <div class="time-slot-header">
              <i class="fa-solid fa-clock"></i>
              <span class="time-slot-title">{{ group.timeSlot }}</span>
              <span class="time-slot-count">{{ group.items.length }}人</span>
            </div>
            <div class="time-slot-items">
              <div
                  v-for="item in group.items"
                  :key="item.appointmentId"
                  class="queue-card"
                  :class="{
                  calling: currentCalling?.appointmentId === item.appointmentId,
                  selected: selectedPatient?.appointmentId === item.appointmentId,
                  paid: item.paid === 1,
                  unpaid: item.paid === 0
                }"
                  @click="selectPatient(item)"
              >
                <div class="queue-number" :class="{ urgent: item.queueNo <= 3 }">
                  {{ item.queueNo || '--' }}
                </div>
                <div class="patient-avatar">
                  <i class="fa-solid fa-user"></i>
                </div>
                <div class="patient-info">
                  <div class="patient-name">{{ item.patientName }}</div>
                  <div class="patient-detail">
                    <span v-if="item.gender === 'M'">男</span>
                    <span v-else-if="item.gender === 'F'">女</span>
                    <span v-else>--</span>
                    <span v-if="item.age"> · {{ item.age }}岁</span>
                  </div>
                  <div class="appointment-no">预约号：{{ item.appointmentNo }}</div>
                </div>
                <div class="time-info">
                  <div class="time-slot">{{ item.timeSlot || '--:--' }}</div>
                  <div class="fee" :class="{ paid: item.paid === 1, unpaid: item.paid === 0 }">
                    ¥{{ item.feeAmount || 50 }}
                    <span class="pay-status">{{ item.paid === 1 ? '已支付' : '未支付' }}</span>
                  </div>
                </div>
                <div class="action-buttons" @click.stop>
                  <el-button
                      v-if="item.paid === 1"
                      type="primary"
                      size="small"
                      :loading="consultingId === item.appointmentId"
                      @click="handleStartConsult(item)"
                  >
                    <i class="fa-solid fa-stethoscope"></i> 开始接诊
                  </el-button>
                  <el-button
                      v-if="item.paid === 0"
                      type="warning"
                      size="small"
                      plain
                      @click="handleRemindPay(item)"
                  >
                    <i class="fa-solid fa-bell"></i> 提醒支付
                  </el-button>
                  <el-button
                      v-if="item.paid === 0"
                      type="danger"
                      size="small"
                      plain
                      @click="handleCancelAppointment(item)"
                  >
                    <i class="fa-solid fa-ban"></i> 取消
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 已完成就诊列表 -->
        <div v-if="activeTab === 'completed' && completedList.length > 0" class="queue-group">
          <div class="group-title">
            <i class="fa-solid fa-check-circle"></i> 已完成就诊
          </div>
          <div
              v-for="item in completedList"
              :key="item.appointmentId"
              class="queue-card completed-card"
          >
            <div class="queue-number">{{ item.queueNo || '--' }}</div>
            <div class="patient-avatar">
              <i class="fa-solid fa-user"></i>
            </div>
            <div class="patient-info">
              <div class="patient-name">{{ item.patientName }}</div>
              <div class="patient-detail">
                <span v-if="item.gender === 'M'">男</span>
                <span v-else-if="item.gender === 'F'">女</span>
                <span v-else>--</span>
                <span v-if="item.age"> · {{ item.age }}岁</span>
              </div>
              <div class="appointment-no">预约号：{{ item.appointmentNo }}</div>
            </div>
            <div class="time-info">
              <div class="time-slot">{{ item.timeSlot || '--:--' }}</div>
              <div class="fee">¥{{ item.feeAmount || 50 }}</div>
            </div>
            <div class="action-buttons" @click.stop>
              <el-button type="info" size="small" plain @click="viewRecord(item)">
                <i class="fa-solid fa-file-lines"></i> 查看病历
              </el-button>
            </div>
          </div>
        </div>

        <!-- 已取消列表 -->
        <div v-if="activeTab === 'cancelled' && cancelledList.length > 0" class="queue-group">
          <div class="group-title cancelled">
            <i class="fa-solid fa-ban"></i> 已取消预约
          </div>
          <div
              v-for="item in cancelledList"
              :key="item.appointmentId"
              class="queue-card cancelled-card"
          >
            <div class="queue-number">{{ item.queueNo || '--' }}</div>
            <div class="patient-avatar">
              <i class="fa-solid fa-user"></i>
            </div>
            <div class="patient-info">
              <div class="patient-name">{{ item.patientName }}</div>
              <div class="patient-detail">
                <span v-if="item.gender === 'M'">男</span>
                <span v-else-if="item.gender === 'F'">女</span>
                <span v-else>--</span>
              </div>
              <div class="appointment-no">预约号：{{ item.appointmentNo }}</div>
            </div>
            <div class="time-info">
              <div class="time-slot">{{ item.timeSlot || '--:--' }}</div>
              <div class="fee">¥{{ item.feeAmount || 50 }}</div>
            </div>
            <div class="status-badge cancelled">已取消</div>
          </div>
        </div>

        <div v-if="isEmpty() && !loading" class="empty-state">
          <i class="fa-solid fa-folder-open"></i>
          <p>{{ getEmptyText() }}</p>
        </div>
      </div>
    </div>

    <!-- 接诊弹窗 -->
    <el-dialog
        v-model="consultDialogVisible"
        width="480px"
        class="consult-dialog"
        :append-to-body="true"
        :close-on-click-modal="false"
        align-center
    >
      <div class="consult-content">
        <div class="consult-header">
          <i class="fa-solid fa-stethoscope"></i>
          <span>开始接诊</span>
        </div>
        <div class="patient-brief">
          <div class="brief-row">
            <span class="label">患者姓名：</span>
            <span class="value">{{ selectedPatient?.patientName }}</span>
          </div>
          <div class="brief-row">
            <span class="label">排队号：</span>
            <span class="value">第 {{ selectedPatient?.queueNo }} 号</span>
          </div>
          <div class="brief-row">
            <span class="label">就诊时段：</span>
            <span class="value">{{ selectedPatient?.timeSlot }}</span>
          </div>
        </div>
        <div class="consult-actions">
          <el-button class="btn-cancel" @click="consultDialogVisible = false">
            取消
          </el-button>
          <el-button class="btn-confirm" :loading="consultingLoading" @click="confirmStartConsult">
            开始接诊
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getQueueList,
  startConsultation,
  callNext,
  getCurrentCalling,
  getTodayStats,
  getAvailableQueueDates,
  doctorCancelAppointment
} from '@/api/admin'

const loading = ref(false)
const refreshing = ref(false)
const allAppointments = ref([])  // 存储所有预约数据
const completedList = ref([])
const cancelledList = ref([])
const stats = ref({
  total: 0,
  waiting: 0,
  completed: 0,
  cancelled: 0
})
const keyword = ref('')
const activeTab = ref('waiting')
const filterStatus = ref(null)
const selectedPatient = ref(null)
const currentCalling = ref(null)
const availableDates = ref([])
const currentDate = ref('')
const waitingQueueCount = ref(0)
const nextWaiting = ref(null)

// 接诊相关
const consultDialogVisible = ref(false)
const consultingLoading = ref(false)
const consultingId = ref(null)

// 叫号相关
const autoCalling = ref(false)

// 时间段排序顺序
const timeSlotOrder = [
  '08:00-09:00',
  '09:00-10:00',
  '10:00-11:00',
  '11:00-12:00',
  '14:00-15:00',
  '15:00-16:00',
  '16:00-17:00',
  '17:00-18:00'
]

// 计算属性 - 只显示已签到的人（有排队号）
const checkedInList = computed(() => {
  return allAppointments.value.filter(item =>
      item.status === 1 &&
      item.paid === 1 &&
      item.queueNo && item.queueNo > 0
  )
})

const waitingCount = computed(() => checkedInList.value.length)

// 按时段分组的待就诊列表
const groupedByTimeSlot = computed(() => {
  const groups = new Map()
  for (const item of checkedInList.value) {
    const slot = item.timeSlot || '其他时段'
    if (!groups.has(slot)) {
      groups.set(slot, [])
    }
    groups.get(slot).push(item)
  }

  const sortedGroups = []
  for (const slot of timeSlotOrder) {
    if (groups.has(slot)) {
      const items = groups.get(slot)
      items.sort((a, b) => (a.queueNo || 0) - (b.queueNo || 0))
      sortedGroups.push({
        timeSlot: slot,
        items: items
      })
      groups.delete(slot)
    }
  }

  for (const [slot, items] of groups) {
    items.sort((a, b) => (a.queueNo || 0) - (b.queueNo || 0))
    sortedGroups.push({
      timeSlot: slot,
      items: items
    })
  }

  return sortedGroups
})

// 获取空状态提示文字
const getEmptyText = () => {
  if (activeTab.value === 'waiting') return '暂无已签到的待就诊患者'
  if (activeTab.value === 'completed') return '暂无已完成就诊记录'
  if (activeTab.value === 'cancelled') return '暂无已取消记录'
  return '暂无数据'
}

const isEmpty = () => {
  if (activeTab.value === 'waiting') return checkedInList.value.length === 0
  if (activeTab.value === 'completed') return completedList.value.length === 0
  if (activeTab.value === 'cancelled') return cancelledList.value.length === 0
  return true
}

// 加载可用日期
const loadAvailableDates = async () => {
  try {
    const res = await getAvailableQueueDates()
    availableDates.value = res.data || res || []
    if (availableDates.value.length > 0) {
      currentDate.value = availableDates.value[0].date
    }
  } catch (error) {
    console.error('加载可用日期失败:', error)
  }
}

// 切换日期
const switchDate = async (date) => {
  currentDate.value = date
  await refreshData()
}

// 设置筛选
const setFilter = (type) => {
  filterStatus.value = type
  if (type === 'waiting') {
    activeTab.value = 'waiting'
    loadQueueList()
  } else if (type === 'completed') {
    activeTab.value = 'completed'
    loadCompletedList()
  } else if (type === 'cancelled') {
    activeTab.value = 'cancelled'
    loadCancelledList()
  } else {
    activeTab.value = 'waiting'
    loadQueueList()
  }
}

// 加载所有预约数据
const loadQueueList = async () => {
  loading.value = true
  try {
    const res = await getQueueList({
      status: 1,
      queryDate: currentDate.value
    })
    allAppointments.value = res.data || res || []
  } catch (error) {
    console.error('加载队列失败:', error)
    ElMessage.error('加载队列失败')
  } finally {
    loading.value = false
  }
}

// 加载已完成列表
const loadCompletedList = async () => {
  loading.value = true
  try {
    const res = await getQueueList({
      status: 2,
      queryDate: currentDate.value
    })
    completedList.value = res.data || res || []
  } catch (error) {
    console.error('加载已完成列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载已取消列表
const loadCancelledList = async () => {
  loading.value = true
  try {
    const res = await getQueueList({
      status: 3,
      queryDate: currentDate.value
    })
    cancelledList.value = res.data || res || []
  } catch (error) {
    console.error('加载已取消列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await getTodayStats(currentDate.value)
    stats.value = res.data || res || {}
    // 覆盖 waiting 为实际已签到人数
    stats.value.waiting = checkedInList.value.length
  } catch (error) {
    console.error('加载统计失败:', error)
  }
}

// 刷新数据
const refreshData = async () => {
  refreshing.value = true
  await Promise.all([loadQueueList(), loadCompletedList(), loadCancelledList(), loadStats(), loadCurrentCalling()])
  refreshing.value = false
  ElMessage.success('刷新成功')
}

// 加载当前叫号信息
const loadCurrentCalling = async () => {
  try {
    const res = await getCurrentCalling(currentDate.value)
    const data = res.data || res
    if (data) {
      currentCalling.value = data.currentCalling || null
      nextWaiting.value = data.nextWaiting || null
      waitingQueueCount.value = data.waitingCount || 0
    }
  } catch (error) {
    console.error('获取当前叫号失败:', error)
  }
}

// 获取最小排队号的患者
const getSmallestQueuePatient = () => {
  if (checkedInList.value.length === 0) return null
  return checkedInList.value.reduce((min, current) =>
      (current.queueNo < min.queueNo) ? current : min, checkedInList.value[0])
}

// 自动叫号
const autoCallNext = async () => {
  const smallestPatient = getSmallestQueuePatient()

  if (!smallestPatient) {
    ElMessage.warning('没有待叫号的患者')
    return
  }

  autoCalling.value = true
  try {
    const res = await callNext(smallestPatient.appointmentId)
    const data = res.data || res
    ElMessage.success({
      message: `请 ${smallestPatient.patientName}（${smallestPatient.queueNo}号，${smallestPatient.timeSlot}）到诊室就诊`,
      duration: 5000,
      showClose: true
    })

    if (data) {
      currentCalling.value = data.currentCalled || null
      nextWaiting.value = data.nextWaiting || null
      waitingQueueCount.value = data.waitingCount || 0
    }
    await refreshData()
  } catch (error) {
    console.error('叫号失败:', error)
    ElMessage.error(error.message || '叫号失败')
  } finally {
    autoCalling.value = false
  }
}

// 选择患者
const selectPatient = (patient) => {
  selectedPatient.value = patient
}

// 开始接诊
const handleStartConsult = (item) => {
  selectedPatient.value = item
  consultDialogVisible.value = true
}

const confirmStartConsult = async () => {
  if (!selectedPatient.value) return
  consultingLoading.value = true
  consultingId.value = selectedPatient.value.appointmentId
  try {
    await startConsultation(selectedPatient.value.appointmentId)
    ElMessage.success(`已接诊 ${selectedPatient.value.patientName}，状态变为已就诊`)
    consultDialogVisible.value = false
    await refreshData()
  } catch (error) {
    console.error('开始接诊失败:', error)
    ElMessage.error(error.message || '开始接诊失败')
  } finally {
    consultingLoading.value = false
    consultingId.value = null
  }
}

// 提醒支付
const handleRemindPay = (item) => {
  ElMessageBox.confirm(
      `是否提醒患者 ${item.patientName} 支付挂号费？`,
      '提醒支付',
      {
        confirmButtonText: '发送提醒',
        cancelButtonText: '取消',
        type: 'info'
      }
  ).then(() => {
    ElMessage.success('已发送支付提醒')
  }).catch(() => {})
}

// 取消预约
const handleCancelAppointment = async (item) => {
  ElMessageBox.confirm(
      `确定要取消患者 ${item.patientName} 的预约吗？`,
      '取消预约',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '返回',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await doctorCancelAppointment(item.appointmentId)
      ElMessage.success('已取消预约')
      await refreshData()
    } catch (error) {
      console.error('取消预约失败:', error)
      ElMessage.error(error.message || '取消预约失败')
    }
  }).catch(() => {})
}

// 查看病历
const viewRecord = (item) => {
  ElMessage.info({
    message: `${item.patientName} 的病历功能开发中，敬请期待`,
    duration: 3000,
    showClose: true
  })
}

// 轮询刷新
let pollTimer = null
const startPolling = () => {
  pollTimer = setInterval(() => {
    if (!consultDialogVisible.value && !autoCalling.value) {
      if (activeTab.value === 'waiting') {
        loadQueueList()
      } else if (activeTab.value === 'completed') {
        loadCompletedList()
      } else if (activeTab.value === 'cancelled') {
        loadCancelledList()
      }
      loadStats()
      loadCurrentCalling()
    }
  }, 30000)
}

onMounted(async () => {
  await loadAvailableDates()
  await refreshData()
  startPolling()
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.queue-page {
  padding: 24px 28px 32px;
  min-height: 100%;
}

.page-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
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
  background: linear-gradient(135deg, #8b6f47, #6b4f2a);
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(107, 79, 42, 0.35);
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

.btn-refresh {
  border-radius: 10px;
  border-color: #e0e0e0;
  color: #5c4a32;
}

.btn-refresh:hover {
  border-color: #8b6f47;
  color: #8b6f47;
}

/* 日期切换标签 */
.date-tabs {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.date-tab {
  flex: 1;
  text-align: center;
  padding: 16px 20px;
  background: rgba(255, 252, 248, 0.95);
  border-radius: 16px;
  border: 1px solid rgba(139, 90, 43, 0.15);
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.date-tab:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(107, 79, 42, 0.1);
}

.date-tab.active {
  border-color: #8b6f47;
  background: linear-gradient(135deg, #8b6f47, #6b4f2a);
  color: #fff;
}

.date-tab.active .date-display,
.date-tab.active .date-week,
.date-tab.active .date-count {
  color: #fff;
}

.date-display {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
}

.date-week {
  font-size: 13px;
  color: #8ba498;
  margin-bottom: 8px;
}

.date-count {
  font-size: 12px;
  color: #2c7a5e;
  background: rgba(44, 122, 94, 0.12);
  display: inline-block;
  padding: 2px 12px;
  border-radius: 20px;
}

.date-tab.active .date-count {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}

.date-count.empty {
  color: #e86a4b;
  background: rgba(232, 106, 75, 0.12);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: rgba(255, 252, 248, 0.95);
  border-radius: 16px;
  border: 1px solid rgba(139, 90, 43, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(107, 79, 42, 0.1);
}

.stat-card.active {
  background: #ffffff;  /* 纯白背景 */
  border-color: #f0e6d8;
  box-shadow: 0 2px 12px rgba(139, 90, 43, 0.1);
}

.stat-icon {
  width: 48px;
  height: 48px;
  line-height: 48px;
  text-align: center;
  font-size: 20px;
  border-radius: 12px;
}

.stat-icon.total {
  background: linear-gradient(135deg, #e8f4f0, #d4e8e0);
  color: #2c7a5e;
}

.stat-icon.waiting {
  background: linear-gradient(135deg, #fff3e0, #ffe8cc);
  color: #e8a54b;
}

.stat-icon.completed {
  background: linear-gradient(135deg, #e8f0ff, #d4e4ff);
  color: #4b7ae8;
}

.stat-icon.cancelled {
  background: linear-gradient(135deg, #ffe8e8, #ffd4d4);
  color: #e84b4b;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a4a3a;
  line-height: 1.2;
}

.stat-value.waiting-value {
  color: #e8a54b;
}

.stat-label {
  font-size: 13px;
  color: #8ba498;
  margin-top: 4px;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 20px;
  flex-wrap: wrap;
}

.search-input {
  width: 300px;
  flex-shrink: 0;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(139, 90, 43, 0.2);
  box-shadow: none;
}

.search-input :deep(.el-input__wrapper:hover),
.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(232, 165, 75, 0.6);
  box-shadow: 0 0 0 2px rgba(232, 165, 75, 0.15);
}

/* 排队信息卡片 */
.queue-info-card {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 8px 24px;
  background: rgba(255, 252, 248, 0.95);
  border-radius: 40px;
  border: 1px solid rgba(139, 90, 43, 0.15);
  flex-wrap: wrap;
}

.queue-info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.queue-info-item .label {
  font-size: 13px;
  color: #8ba498;
}

.queue-info-item .value {
  font-size: 24px;
  font-weight: 700;
  color: #6b4f2a;
}

.queue-info-item .value.current-queue {
  font-size: 32px;
  color: #e8a54b;
  background: rgba(232, 165, 75, 0.15);
  padding: 0 12px;
  border-radius: 30px;
}

.queue-info-item .value.waiting-count {
  font-size: 24px;
  color: #e86a4b;
}

.queue-info-item .name {
  font-size: 14px;
  color: #8b6f47;
  font-weight: 500;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-slot-badge {
  font-size: 11px;
  color: #fff;
  background: #8b6f47;
  padding: 2px 8px;
  border-radius: 20px;
  margin-left: 6px;
}

.call-btn {
  border-radius: 40px;
  background: linear-gradient(135deg, #8b6f47, #6b4f2a);
  border: none;
  color: #fff;
  padding: 8px 20px;
}

.call-btn:hover {
  background: linear-gradient(135deg, #6b4f2a, #5a3f1f);
  color: #fff;
}

.call-btn:disabled {
  background: #ccc;
  color: #fff;
}

/* 队列标签页 */
.queue-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(139, 90, 43, 0.15);
  padding-bottom: 0;
}

.tab-item {
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 500;
  color: #8ba498;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tab-item:hover {
  color: #8b6f47;
}

.tab-item.active {
  color: #8b6f47;
  border-bottom-color: #8b6f47;
}

.badge {
  background: rgba(139, 90, 43, 0.12);
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

/* 队列列表 */
.queue-container {
  background: rgba(255, 252, 248, 0.95);
  border-radius: 16px;
  padding: 20px 24px;
  min-height: 500px;
  border: 1px solid rgba(139, 90, 43, 0.15);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.queue-group {
  margin-bottom: 24px;
}

.group-title {
  font-size: 14px;
  font-weight: 600;
  color: #8b6f47;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(139, 90, 43, 0.15);
  display: flex;
  align-items: center;
  gap: 8px;
}

.group-title.cancelled {
  color: #e84b4b;
}

/* 时间段分组样式 */
.time-slot-group {
  margin-bottom: 24px;
  border: 1px solid rgba(139, 90, 43, 0.15);
  border-radius: 16px;
  overflow: hidden;
}

.time-slot-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #8b6f47, #6b4f2a);
  color: #fff;
}

.time-slot-header i {
  font-size: 16px;
}

.time-slot-title {
  font-size: 16px;
  font-weight: 600;
}

.time-slot-count {
  font-size: 12px;
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 10px;
  border-radius: 20px;
  margin-left: auto;
}

.time-slot-items {
  padding: 12px;
  background: #fff;
}

.queue-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid rgba(139, 90, 43, 0.1);
  border-radius: 12px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.queue-card:last-child {
  margin-bottom: 0;
}

.queue-card:hover {
  border-color: #8b6f47;
  box-shadow: 0 2px 8px rgba(139, 90, 43, 0.1);
  transform: translateX(4px);
}

.queue-card.calling {
  background: linear-gradient(135deg, #f5efe8, #ede4d9);
  border-color: #8b6f47;
}

.queue-card.selected {
  border-color: #8b6f47;
  box-shadow: 0 4px 12px rgba(139, 90, 43, 0.15);
}

.queue-card.paid {
  border-left: 4px solid #8b6f47;
}

.queue-card.unpaid {
  border-left: 4px solid #e86a4b;
  background: #fffaf5;
}

.queue-card.completed-card {
  background: #f5f5f5;
  cursor: default;
  border-left: 4px solid #4b7ae8;
}

.queue-card.completed-card:hover {
  transform: none;
}

.queue-card.cancelled-card {
  background: #fff5f5;
  border-color: rgba(232, 75, 75, 0.2);
  cursor: default;
  border-left: 4px solid #e84b4b;
}

.queue-card.cancelled-card:hover {
  transform: none;
}

.queue-number {
  width: 50px;
  height: 50px;
  line-height: 50px;
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  background: #f5efe8;
  border-radius: 14px;
  color: #8b6f47;
  flex-shrink: 0;
}

.queue-number.urgent {
  background: linear-gradient(135deg, #e8a54b, #d48232);
  color: #fff;
}

.patient-avatar {
  width: 48px;
  height: 48px;
  line-height: 48px;
  text-align: center;
  font-size: 20px;
  background: linear-gradient(135deg, #f5efe8, #ede4d9);
  border-radius: 50%;
  color: #8b6f47;
  flex-shrink: 0;
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
  color: #8ba498;
  margin-bottom: 4px;
}

.appointment-no {
  font-size: 11px;
  color: #b0a088;
}

.time-info {
  text-align: right;
  min-width: 120px;
  flex-shrink: 0;
}

.time-slot {
  font-size: 13px;
  font-weight: 500;
  color: #8b6f47;
  margin-bottom: 4px;
}

.fee {
  font-size: 14px;
  font-weight: 700;
  color: #e8a54b;
}

.fee.paid {
  color: #8b6f47;
}

.fee.unpaid {
  color: #e86a4b;
}

.pay-status {
  font-size: 11px;
  font-weight: normal;
  margin-left: 6px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-badge.cancelled {
  background: #ffe8e8;
  color: #e84b4b;
}

/* 接诊弹窗 */
.consult-dialog :deep(.el-dialog) {
  border-radius: 20px;
  background: rgba(255, 252, 248, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(139, 90, 43, 0.2);
}

.consult-content {
  text-align: center;
  padding: 20px 24px 28px;
}

.consult-header {
  font-size: 18px;
  font-weight: 600;
  color: #8b6f47;
  margin-bottom: 24px;
}

.consult-header i {
  font-size: 24px;
  margin-right: 8px;
}

.patient-brief {
  background: #f5efe8;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 28px;
  text-align: left;
}

.brief-row {
  display: flex;
  margin-bottom: 12px;
}

.brief-row:last-child {
  margin-bottom: 0;
}

.brief-row .label {
  width: 80px;
  color: #8ba498;
}

.brief-row .value {
  flex: 1;
  color: #2c1810;
  font-weight: 500;
}

.consult-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.btn-cancel {
  border-radius: 10px;
  border-color: #e0e0e0;
  color: #8ba498;
}

.btn-confirm {
  border-radius: 10px;
  background: linear-gradient(135deg, #8b6f47, #6b4f2a);
  border: none;
  color: #fff;
}

.btn-confirm:hover {
  background: linear-gradient(135deg, #6b4f2a, #5a3f1f);
  color: #fff;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #b0a088;
}

.empty-state i {
  font-size: 56px;
  margin-bottom: 16px;
}
</style>